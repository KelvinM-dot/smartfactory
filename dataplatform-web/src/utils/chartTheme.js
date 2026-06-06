/** ECharts 工业暗色主题 */
export const industrialChartTheme = {
  backgroundColor: 'transparent',
  textStyle: { color: '#94a3b8', fontFamily: 'Roboto Mono, monospace', fontSize: 11 },
  legend: { textStyle: { color: '#94a3b8' }, top: 4 },
  grid: { left: 48, right: 16, top: 36, bottom: 28 },
  xAxis: {
    axisLine: { lineStyle: { color: '#334155' } },
    axisLabel: { color: '#64748b', fontSize: 10 },
    splitLine: { show: false }
  },
  yAxis: {
    axisLine: { show: false },
    axisLabel: { color: '#64748b', fontSize: 10 },
    splitLine: { lineStyle: { color: '#1e293b', type: 'dashed' } }
  },
  tooltip: {
    backgroundColor: '#1e293b',
    borderColor: '#334155',
    textStyle: { color: '#e2e8f0', fontSize: 12 }
  }
}

export function buildTrendOption(seriesList) {
  const colors = ['#22d3ee', '#34d399', '#fbbf24', '#f87171', '#a78bfa', '#fb923c']
  const series = (seriesList || []).map((s, idx) => ({
    name: `${s.display_name || s.field_id}${s.unit ? ` (${s.unit})` : ''}`,
    type: 'line',
    showSymbol: false,
    smooth: true,
    lineStyle: { width: 1.5 },
    itemStyle: { color: colors[idx % colors.length] },
    data: (s.points || []).map(p => [p.t, p.v]),
    markArea: s.spec_limits?.lsl != null ? {
      silent: true,
      itemStyle: { color: 'rgba(52, 211, 153, 0.06)' },
      data: [[{ yAxis: s.spec_limits.lsl }, { yAxis: s.spec_limits.usl }]]
    } : undefined
  }))

  return {
    ...industrialChartTheme,
    tooltip: { ...industrialChartTheme.tooltip, trigger: 'axis' },
    legend: { ...industrialChartTheme.legend, type: 'scroll' },
    xAxis: { ...industrialChartTheme.xAxis, type: 'time' },
    yAxis: { ...industrialChartTheme.yAxis, type: 'value', scale: true },
    series
  }
}

export function buildSparkOption(points, color = '#22d3ee', limits) {
  return {
    backgroundColor: 'transparent',
    grid: { left: 0, right: 0, top: 2, bottom: 2 },
    xAxis: { type: 'time', show: false },
    yAxis: { type: 'value', show: false, scale: true },
    series: [{
      type: 'line',
      showSymbol: false,
      smooth: true,
      lineStyle: { width: 1, color },
      areaStyle: { color: `${color}22` },
      data: (points || []).map(p => [p.t, p.v]),
      markLine: limits?.lsl != null ? {
        silent: true,
        symbol: 'none',
        lineStyle: { color: '#ef4444', type: 'dashed', width: 1 },
        data: [{ yAxis: limits.usl }, { yAxis: limits.lsl }]
      } : undefined
    }]
  }
}
