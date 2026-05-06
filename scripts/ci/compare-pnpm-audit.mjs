import fs from 'node:fs'

function parseArgs(argv) {
  const args = { base: '', head: '' }

  for (let i = 2; i < argv.length; i += 1) {
    const current = argv[i]
    const next = argv[i + 1]

    if (current === '--base' && next) {
      args.base = next
      i += 1
      continue
    }

    if (current === '--head' && next) {
      args.head = next
      i += 1
      continue
    }
  }

  if (!args.base || !args.head) {
    throw new Error('Usage: node scripts/ci/compare-pnpm-audit.mjs --base <file> --head <file>')
  }

  return args
}

function loadReport(filePath) {
  return JSON.parse(fs.readFileSync(filePath, 'utf8'))
}

function extractFindings(report) {
  const advisories = Object.values(report.advisories ?? {})
  const findings = []

  for (const advisory of advisories) {
    const severity = advisory.severity ?? 'unknown'

    if (!['high', 'critical'].includes(severity)) {
      continue
    }

    for (const finding of advisory.findings ?? []) {
      for (const path of finding.paths ?? []) {
        findings.push({
          id: `${advisory.id}:${path}`,
          advisoryId: advisory.id,
          severity,
          moduleName: advisory.module_name,
          title: advisory.title,
          path,
          recommendation: advisory.recommendation,
          url: advisory.url,
        })
      }
    }
  }

  return findings
}

function indexById(findings) {
  return new Map(findings.map((finding) => [finding.id, finding]))
}

try {
  const { base, head } = parseArgs(process.argv)
  const baseFindings = extractFindings(loadReport(base))
  const headFindings = extractFindings(loadReport(head))
  const baseIndex = indexById(baseFindings)
  const newFindings = headFindings.filter((finding) => !baseIndex.has(finding.id))
  const resolvedCount = baseFindings.length - headFindings.length + newFindings.length

  if (newFindings.length === 0) {
    console.log(
      `No new high/critical pnpm audit findings compared with base branch (${headFindings.length} current, ${baseFindings.length} base).`,
    )
    if (resolvedCount > 0) {
      console.log(`Resolved findings in this branch: ${resolvedCount}`)
    }
    process.exit(0)
  }

  console.error('New high/critical pnpm audit findings compared with base branch:')
  for (const finding of newFindings) {
    console.error(`- [${finding.severity}] ${finding.moduleName} at ${finding.path}`)
    console.error(`  ${finding.title}`)
    if (finding.recommendation) {
      console.error(`  ${finding.recommendation}`)
    }
    if (finding.url) {
      console.error(`  ${finding.url}`)
    }
  }
  process.exit(1)
} catch (error) {
  console.error(error instanceof Error ? error.message : String(error))
  process.exit(1)
}
