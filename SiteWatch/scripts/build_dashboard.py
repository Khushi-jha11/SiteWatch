#!/usr/bin/env python3
"""
Builds docs/index.html: a static dashboard from docs/data/history.csv.
Runs after the Maven/TestNG suite on every CI run, and can be run locally too:

    python scripts/build_dashboard.py
"""
import json
from datetime import datetime, timezone
from pathlib import Path

import pandas as pd
import plotly.graph_objects as go
import plotly.io as pio

ROOT = Path(__file__).resolve().parent.parent
CSV_PATH = ROOT / "docs" / "data" / "history.csv"
OUT_PATH = ROOT / "docs" / "index.html"

PLOTLY_TEMPLATE = "plotly_dark"
COLORS = {
    "PASS": "#3fb950",
    "FAIL": "#f85149",
    "accent": "#58a6ff",
    "bg": "#0d1117",
    "card": "#161b22",
    "border": "#30363d",
    "text": "#c9d1d9",
    "muted": "#8b949e",
}


def load_history() -> pd.DataFrame:
    if not CSV_PATH.exists() or CSV_PATH.stat().st_size == 0:
        return pd.DataFrame(columns=[
            "timestamp", "site", "url", "status", "load_time_ms", "title",
            "elements_found", "elements_total", "links_checked", "links_broken",
            "notes", "screenshot",
        ])
    df = pd.read_csv(CSV_PATH)
    df["timestamp"] = pd.to_datetime(df["timestamp"], errors="coerce", utc=True)
    df = df.dropna(subset=["timestamp"]).sort_values("timestamp")
    for col in ("load_time_ms", "elements_found", "elements_total", "links_checked", "links_broken"):
        df[col] = pd.to_numeric(df[col], errors="coerce").fillna(0)
    return df


def latest_snapshot(df: pd.DataFrame) -> pd.DataFrame:
    if df.empty:
        return df
    return df.sort_values("timestamp").groupby("site", as_index=False).tail(1)


def fig_to_div(fig: go.Figure, div_id: str) -> str:
    fig.update_layout(
        template=PLOTLY_TEMPLATE,
        paper_bgcolor=COLORS["card"],
        plot_bgcolor=COLORS["card"],
        font=dict(color=COLORS["text"], family="Inter, -apple-system, sans-serif", size=13),
        margin=dict(l=48, r=24, t=16, b=40),
        legend=dict(orientation="h", yanchor="bottom", y=1.02, xanchor="left", x=0),
        hoverlabel=dict(bgcolor=COLORS["bg"], font_size=12),
    )
    fig.update_xaxes(gridcolor=COLORS["border"], zerolinecolor=COLORS["border"])
    fig.update_yaxes(gridcolor=COLORS["border"], zerolinecolor=COLORS["border"])
    return pio.to_html(fig, include_plotlyjs=False, full_html=False, div_id=div_id, config={"displayModeBar": False})


def load_time_trend(df: pd.DataFrame) -> str:
    fig = go.Figure()
    if not df.empty:
        for site, group in df.groupby("site"):
            fig.add_trace(go.Scatter(
                x=group["timestamp"], y=group["load_time_ms"],
                mode="lines+markers", name=site, line=dict(width=2), marker=dict(size=5),
            ))
    fig.update_yaxes(title_text="Load time (ms)")
    return fig_to_div(fig, "load-time-trend")


def failure_rate_chart(df: pd.DataFrame) -> str:
    fig = go.Figure()
    if not df.empty:
        summary = df.groupby("site")["status"].apply(lambda s: (s == "FAIL").mean() * 100).reset_index()
        summary.columns = ["site", "fail_pct"]
        summary = summary.sort_values("fail_pct", ascending=True)
        colors = [COLORS["FAIL"] if v > 0 else COLORS["PASS"] for v in summary["fail_pct"]]
        fig.add_trace(go.Bar(
            x=summary["fail_pct"], y=summary["site"], orientation="h",
            marker_color=colors, text=[f"{v:.0f}%" for v in summary["fail_pct"]], textposition="outside",
        ))
    fig.update_xaxes(title_text="Failure rate across all runs (%)", range=[0, 100])
    return fig_to_div(fig, "failure-rate")


def broken_links_chart(df: pd.DataFrame) -> str:
    fig = go.Figure()
    if not df.empty:
        latest = latest_snapshot(df)
        fig.add_trace(go.Bar(x=latest["site"], y=latest["links_broken"], marker_color=COLORS["accent"], name="Broken"))
        fig.add_trace(go.Bar(x=latest["site"], y=latest["links_checked"] - latest["links_broken"],
                              marker_color=COLORS["border"], name="Healthy"))
    fig.update_layout(barmode="stack")
    fig.update_yaxes(title_text="Links (most recent run)")
    return fig_to_div(fig, "broken-links")


def status_dot(status: str) -> str:
    color = COLORS.get(status, COLORS["muted"])
    return f'<span class="dot" style="background:{color}"></span>{status}'


def build_site_cards(latest: pd.DataFrame) -> str:
    if latest.empty:
        return '<p class="muted">No runs recorded yet. Trigger the workflow or run the suite locally to populate this dashboard.</p>'

    cards = []
    for _, row in latest.sort_values("site").iterrows():
        raw_notes = row.get("notes")
        notes = "" if pd.isna(raw_notes) else str(raw_notes).strip()
        notes_html = f'<p class="notes">{notes}</p>' if notes else ""

        raw_shot = row.get("screenshot")
        screenshot = "" if pd.isna(raw_shot) else str(raw_shot).strip()
        shot_html = (
            f'<a class="shot-link" href="{screenshot}" target="_blank">View failure screenshot →</a>'
            if screenshot else ""
        )
        cards.append(f"""
        <article class="card site-card status-{row['status'].lower()}">
          <header>
            <h3>{row['site']}</h3>
            <span class="status">{status_dot(row['status'])}</span>
          </header>
          <a class="url" href="{row['url']}" target="_blank">{row['url']}</a>
          <div class="metrics">
            <div><span class="metric-value">{int(row['load_time_ms'])}</span><span class="metric-label">ms load</span></div>
            <div><span class="metric-value">{int(row['elements_found'])}/{int(row['elements_total'])}</span><span class="metric-label">elements</span></div>
            <div><span class="metric-value">{int(row['links_broken'])}/{int(row['links_checked'])}</span><span class="metric-label">broken links</span></div>
          </div>
          {notes_html}
          {shot_html}
        </article>""")
    return "\n".join(cards)


def build_html(df: pd.DataFrame) -> str:
    latest = latest_snapshot(df)
    total_sites = latest["site"].nunique() if not latest.empty else 0
    passing = int((latest["status"] == "PASS").sum()) if not latest.empty else 0
    avg_load = f"{latest['load_time_ms'].mean():.0f}" if not latest.empty else "–"
    total_runs = df["timestamp"].nunique() if not df.empty else 0
    updated = datetime.now(timezone.utc).strftime("%d %b %Y, %H:%M UTC")

    return f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>SiteWatch — Automated Website Health Dashboard</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@500&display=swap" rel="stylesheet">
<script src="https://cdn.jsdelivr.net/npm/plotly.js@2.32.0/dist/plotly.min.js"></script>
<style>
  :root {{
    --bg: {COLORS['bg']}; --card: {COLORS['card']}; --border: {COLORS['border']};
    --text: {COLORS['text']}; --muted: {COLORS['muted']}; --accent: {COLORS['accent']};
    --pass: {COLORS['PASS']}; --fail: {COLORS['FAIL']};
  }}
  * {{ box-sizing: border-box; }}
  body {{
    margin: 0; background: var(--bg); color: var(--text);
    font-family: 'Inter', -apple-system, sans-serif; line-height: 1.5;
  }}
  header.hero {{
    padding: 56px 24px 40px; max-width: 1080px; margin: 0 auto;
    border-bottom: 1px solid var(--border);
  }}
  .eyebrow {{ color: var(--accent); font-family: 'JetBrains Mono', monospace; font-size: 13px; letter-spacing: 0.02em; }}
  h1 {{ font-size: 40px; margin: 8px 0 12px; font-weight: 700; letter-spacing: -0.02em; }}
  .tagline {{ color: var(--muted); font-size: 16px; max-width: 640px; }}
  main {{ max-width: 1080px; margin: 0 auto; padding: 40px 24px 80px; }}
  .stat-row {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 16px; margin-bottom: 40px; }}
  .stat {{ background: var(--card); border: 1px solid var(--border); border-radius: 10px; padding: 18px 20px; }}
  .stat .value {{ font-family: 'JetBrains Mono', monospace; font-size: 28px; font-weight: 500; }}
  .stat .label {{ color: var(--muted); font-size: 13px; margin-top: 4px; }}
  section {{ margin-bottom: 48px; }}
  section h2 {{ font-size: 20px; margin-bottom: 16px; font-weight: 600; }}
  .card {{ background: var(--card); border: 1px solid var(--border); border-radius: 10px; padding: 20px; }}
  .charts-grid {{ display: grid; grid-template-columns: 1fr; gap: 20px; }}
  .cards-grid {{ display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 16px; }}
  .site-card header {{ display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }}
  .site-card h3 {{ margin: 0; font-size: 17px; }}
  .site-card.status-fail {{ border-color: rgba(248,81,73,0.5); }}
  .status {{ font-family: 'JetBrains Mono', monospace; font-size: 12px; display: flex; align-items: center; gap: 6px; }}
  .dot {{ width: 8px; height: 8px; border-radius: 50%; display: inline-block; }}
  .url {{ color: var(--muted); font-size: 13px; text-decoration: none; word-break: break-all; }}
  .url:hover {{ color: var(--accent); }}
  .metrics {{ display: flex; gap: 20px; margin: 16px 0 8px; }}
  .metric-value {{ display: block; font-family: 'JetBrains Mono', monospace; font-size: 18px; }}
  .metric-label {{ display: block; color: var(--muted); font-size: 11px; margin-top: 2px; }}
  .notes {{ font-size: 13px; color: var(--fail); margin: 10px 0 0; }}
  .shot-link {{ display: inline-block; margin-top: 10px; font-size: 13px; color: var(--accent); text-decoration: none; }}
  .muted {{ color: var(--muted); }}
  footer {{ max-width: 1080px; margin: 0 auto; padding: 24px; color: var(--muted); font-size: 13px; border-top: 1px solid var(--border); }}
  footer a {{ color: var(--accent); text-decoration: none; }}
  @media (min-width: 860px) {{ .charts-grid {{ grid-template-columns: 1fr 1fr; }} .charts-grid > :first-child {{ grid-column: 1 / -1; }} }}
</style>
</head>
<body>
<header class="hero">
  <div class="eyebrow">sitewatch</div>
  <h1>Automated website health monitor</h1>
  <p class="tagline">Selenium and TestNG check page load, key elements, and links on a schedule.
  GitHub Actions runs the suite and republishes this page automatically.</p>
</header>
<main>
  <div class="stat-row">
    <div class="stat"><div class="value">{total_sites}</div><div class="label">sites monitored</div></div>
    <div class="stat"><div class="value">{passing}/{total_sites if total_sites else 0}</div><div class="label">passing right now</div></div>
    <div class="stat"><div class="value">{avg_load}<span style="font-size:15px;color:var(--muted)"> ms</span></div><div class="label">avg load time</div></div>
    <div class="stat"><div class="value">{total_runs}</div><div class="label">recorded runs</div></div>
  </div>

  <section>
    <h2>Current status</h2>
    <div class="cards-grid">
      {build_site_cards(latest)}
    </div>
  </section>

  <section>
    <h2>Trends</h2>
    <div class="charts-grid">
      <div class="card">{load_time_trend(df)}</div>
      <div class="card">{failure_rate_chart(df)}</div>
      <div class="card">{broken_links_chart(df)}</div>
    </div>
  </section>
</main>
<footer>
  Last updated {updated} · Built with Selenium, TestNG, Maven and GitHub Actions ·
  <a href="https://github.com/" target="_blank">View source on GitHub</a>
</footer>
</body>
</html>"""


def main():
    df = load_history()
    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    OUT_PATH.write_text(build_html(df), encoding="utf-8")
    print(f"Dashboard written to {OUT_PATH} ({len(df)} history rows)")


if __name__ == "__main__":
    main()
