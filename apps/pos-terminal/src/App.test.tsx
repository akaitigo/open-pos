import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { App } from "./App";

describe("App", () => {
  it("クラッシュせずにレンダリングできる", () => {
    render(<App />);

    expect(screen.getByText("OpenPOS Terminal")).toBeInTheDocument();
  });

  it("サブタイトルが表示される", () => {
    render(<App />);

    expect(screen.getByText("POS端末アプリケーション")).toBeInTheDocument();
  });
});
