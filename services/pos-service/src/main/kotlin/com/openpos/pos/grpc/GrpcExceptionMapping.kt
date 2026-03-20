package com.openpos.pos.grpc

import io.grpc.Status
import io.grpc.StatusRuntimeException

/**
 * ビジネス例外階層と gRPC Status Code のマッピング。
 *
 * - INVALID_ARGUMENT: 入力バリデーションエラー（UUID 不正、必須フィールド欠落等）
 * - FAILED_PRECONDITION: ビジネスロジックエラー（状態遷移違反、残高不足等）
 * - NOT_FOUND: リソース未存在
 */

/** 入力バリデーションエラー → INVALID_ARGUMENT */
open class InvalidInputException(
    message: String,
) : RuntimeException(message)

/** ビジネスロジック前提条件違反 → FAILED_PRECONDITION */
open class BusinessPreconditionException(
    message: String,
) : RuntimeException(message)

/** リソース未存在 → NOT_FOUND */
open class ResourceNotFoundException(
    message: String,
) : RuntimeException(message)

/**
 * ビジネス例外を適切な gRPC StatusRuntimeException に変換する。
 */
fun mapToGrpcException(e: Exception): StatusRuntimeException =
    when (e) {
        is InvalidInputException -> {
            Status.INVALID_ARGUMENT.withDescription(e.message).asRuntimeException()
        }

        is ResourceNotFoundException -> {
            Status.NOT_FOUND.withDescription(e.message).asRuntimeException()
        }

        is BusinessPreconditionException -> {
            Status.FAILED_PRECONDITION.withDescription(e.message).asRuntimeException()
        }

        is IllegalArgumentException -> {
            Status.INVALID_ARGUMENT.withDescription(e.message).asRuntimeException()
        }

        is IllegalStateException -> {
            Status.FAILED_PRECONDITION.withDescription(e.message).asRuntimeException()
        }

        is StatusRuntimeException -> {
            e
        }

        else -> {
            val log =
                org.jboss.logging.Logger
                    .getLogger("GrpcExceptionMapping")
            log.errorf(e, "Unhandled exception in gRPC handler")
            Status.INTERNAL.withDescription("Internal server error").asRuntimeException()
        }
    }
