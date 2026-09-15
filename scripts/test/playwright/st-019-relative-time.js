// ST-019: boundary check for the 4-week relative-time display cap.
// Run via the official Playwright MCP (mcp__playwright__browser_run_code_unsafe), page already
// navigated to any same-origin Band Link page so the ES module import below resolves.
// Exercises the REAL src/main/resources/static/js/ui.js#relativeTime (and loginRelativeTime)
// functions in-browser with synthetic timestamps built off the live Date.now() at call time -
// no server data, no seed dependency, no Date mocking required since the function itself reads
// Date.now() internally and we just vary the input timestamp's offset from "now".
async (page) => {
  const result = await page.evaluate(async () => {
    const mod = await import('/js/ui.js');
    const { relativeTime, loginRelativeTime } = mod;
    const ms = { min: 60000, hour: 3600000, day: 86400000, week: 604800000 };
    const ago = (offsetMs) => new Date(Date.now() - offsetMs).toISOString();
    const cases = [
      ['30秒前 -> 1分前(切り上げ最小値)', ago(30 * 1000), '1分前'],
      ['59分前', ago(59 * ms.min), '59分前'],
      ['60分前(1時間境界)', ago(60 * ms.min), '1時間前'],
      ['23時間前', ago(23 * ms.hour), '23時間前'],
      ['24時間前(1日境界)', ago(24 * ms.hour), '1日前'],
      ['6日前', ago(6 * ms.day), '6日前'],
      ['7日前(1週境界)', ago(7 * ms.day), '1週間前'],
      ['21日前(3週)', ago(21 * ms.day), '3週間前'],
      ['27日前(3週6日、4週未満)', ago(27 * ms.day), '3週間前'],
      ['28日前(4週境界)', ago(28 * ms.day), '4週間前'],
      ['35日前(4週超)', ago(35 * ms.day), '4週間前'],
      ['400日前(4週超・上限維持)', ago(400 * ms.day), '4週間前'],
    ];
    const results = cases.map(([label, value, expected]) => {
      const actual = relativeTime(value);
      return { label, value, expected, actual, pass: actual === expected };
    });
    // loginRelativeTime has no 4-week cap (it continues into month buckets) - a boundary
    // regression here would mean the cap leaked into the wrong function.
    const loginNoCap = loginRelativeTime(ago(35 * ms.day));
    results.push({
      label: 'loginRelativeTime(35日前) は4週間上限を持たない',
      value: ago(35 * ms.day),
      expected: '(月表示、"4週間前"ではない)',
      actual: loginNoCap,
      pass: loginNoCap !== '4週間前',
    });
    return { results, allPass: results.every((r) => r.pass) };
  });
  return result;
}
