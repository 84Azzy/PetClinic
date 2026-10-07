<template>
  <div class="dashboard">
    <PageHeader title="诊所工作台"
      ><div class="workbench-date">{{ today }}</div>
      <el-button
        v-if="auth.canVisitPath('/visits')"
        @click="$router.push('/visits')"
        ><el-icon><Calendar /></el-icon>查看预约</el-button
      ></PageHeader
    >
    <PageState
      v-if="errors.summary"
      kind="error"
      title="统计暂时无法加载"
      @retry="load"
    />
    <section
      v-else
      class="summary-strip"
      aria-label="诊所数据摘要"
      :aria-busy="loading"
    >
      <div v-for="stat in stats" :key="stat.label" class="summary-item">
        <span
          :class="['summary-icon', { amber: stat.key === 'scheduledVisits' }]"
          ><ClinicIcon :name="stat.icon"
        /></span>
        <div>
          <span class="summary-label">{{ stat.label }}</span
          ><strong>{{ summary ? summary[stat.key] : "—" }}</strong>
        </div>
      </div>
    </section>
    <div class="workbench-grid">
      <section class="panel appointments-panel">
        <div class="panel-head">
          <h2>近期预约</h2>
          <el-button
            v-if="auth.canVisitPath('/visits')"
            link
            type="primary"
            @click="$router.push('/visits')"
            >全部预约<el-icon><ArrowRight /></el-icon
          ></el-button>
        </div>
        <PageState v-if="errors.recent" kind="error" @retry="load" />
        <PageState v-else-if="loading && !recent.length" kind="loading" />
        <el-table v-else :data="recent" class="appointment-table desktop-table">
          <el-table-column label="宠物" min-width="170"
            ><template #default="{ row }"
              ><div class="patient-cell">
                <PetAvatar
                  :name="row.petName"
                  :src="petForVisit(row.id)?.photoUrl"
                  :size="56"
                />
                <div>
                  <strong>{{ row.petName }}</strong
                  ><span
                    v-if="petForVisit(row.id)?.breed"
                    class="cell-secondary"
                    >{{ petForVisit(row.id)?.breed }}</span
                  >
                </div>
              </div></template
            ></el-table-column
          >
          <el-table-column prop="vetName" label="接诊兽医" min-width="110" />
          <el-table-column label="预约时间" min-width="155"
            ><template #default="{ row }">{{
              formatDate(row.startTime, true)
            }}</template></el-table-column
          >
          <el-table-column label="状态" width="110"
            ><template #default="{ row }"
              ><StatusTag :status="row.status" /></template
          ></el-table-column>
          <el-table-column v-if="auth.canVisitPath('/visits')" width="60"
            ><template #default="{ row }"
              ><el-button
                link
                type="primary"
                :aria-label="'查看' + row.petName + '的预约'"
                @click="
                  $router.push({ path: '/visits', query: { visitId: row.id } })
                "
                ><el-icon><ArrowRight /></el-icon></el-button></template
          ></el-table-column>
          <template #empty
            ><PageState
              kind="empty"
              title="还没有预约记录"
              description="已有预约会按创建顺序显示在这里。"
          /></template>
        </el-table>
        <div v-if="!errors.recent && !loading" class="mobile-records">
          <article
            v-for="row in recent"
            :key="row.id"
            class="mobile-record-card"
          >
            <div class="panel-head">
              <div class="patient-cell">
                <PetAvatar
                  :name="row.petName"
                  :src="petForVisit(row.id)?.photoUrl"
                  :size="44"
                /><strong>{{ row.petName }}</strong>
              </div>
              <StatusTag :status="row.status" />
            </div>
            <p>{{ row.vetName }} · {{ formatDate(row.startTime, true) }}</p>
            <el-button
              v-if="auth.canVisitPath('/visits')"
              type="primary"
              plain
              @click="
                $router.push({ path: '/visits', query: { visitId: row.id } })
              "
              >查看预约</el-button
            >
          </article>
          <PageState v-if="!recent.length" kind="empty" title="暂无预约记录" />
        </div>
      </section>
      <div class="analysis-column">
        <section class="panel">
          <div class="panel-head">
            <div>
              <h2>近七日新建预约</h2>
              <p>按预约创建日期统计</p>
            </div>
          </div>
          <PageState v-if="errors.trend" kind="error" @retry="load" />
          <div
            v-show="!errors.trend"
            ref="trendRef"
            class="trend-chart"
            role="img"
            :aria-label="trendDescription"
          />
        </section>
        <section class="panel">
          <div class="panel-head">
            <div>
              <h2>兽医预约量</h2>
              <p>累计预约次数，包含各预约状态</p>
            </div>
          </div>
          <PageState v-if="errors.workload" kind="error" @retry="load" />
          <PageState
            v-else-if="!loading && !workload.length"
            kind="empty"
            title="暂无预约统计"
          />
          <div v-else class="workload-list">
            <div
              v-for="item in workload"
              :key="item.label"
              class="workload-row"
            >
              <span>{{ item.label }}</span>
              <div class="workload-track">
                <i
                  :style="{
                    width: (Number(item.value) / maxWorkload) * 100 + '%',
                  }"
                />
              </div>
              <span class="workload-value">{{ item.value }}</span>
            </div>
          </div>
        </section>
      </div>
    </div>
    <section class="panel distribution-panel">
      <div class="panel-head">
        <h2>宠物类型分布</h2>
        <span class="caption">按已有宠物档案统计</span>
      </div>
      <PageState v-if="errors.distribution" kind="error" @retry="load" />
      <PageState
        v-else-if="!loading && !distributionTotal"
        kind="empty"
        title="还没有宠物档案"
      />
      <template v-else>
        <div
          class="distribution-bar"
          role="img"
          :aria-label="distributionDescription"
        >
          <span
            v-for="(item, index) in distribution"
            :key="item.label"
            :style="{
              width: (Number(item.value) / distributionTotal) * 100 + '%',
              background: palette[index % palette.length],
            }"
            ><b v-if="Number(item.value) / distributionTotal > 0.12"
              >{{
                Math.round((Number(item.value) / distributionTotal) * 100)
              }}%</b
            ></span
          >
        </div>
        <ul class="distribution-legend">
          <li v-for="(item, index) in distribution" :key="item.label">
            <i
              :style="{ background: palette[index % palette.length] }"
            /><span>{{ item.label }}</span
            ><strong>{{ item.value }}只</strong>
          </li>
        </ul>
      </template>
    </section>
  </div>
</template>
<script setup lang="ts">
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
} from "vue";
import { ArrowRight, Calendar } from "@element-plus/icons-vue";
import * as echarts from "echarts/core";
import { LineChart } from "echarts/charts";
import { GridComponent, TooltipComponent } from "echarts/components";
import { CanvasRenderer } from "echarts/renderers";
echarts.use([LineChart, GridComponent, TooltipComponent, CanvasRenderer]);
import PageHeader from "@/components/PageHeader.vue";
import PageState from "@/components/PageState.vue";
import StatusTag from "@/components/StatusTag.vue";
import PetAvatar from "@/components/PetAvatar.vue";
import ClinicIcon from "@/components/ClinicIcon.vue";
import {
  getDashboardSummary,
  getVisitTrend,
  getPetDistribution,
  getVetWorkload,
  getRecentVisits,
  type DashboardSummary,
  type MetricPoint,
  type RecentVisit,
} from "@/api/dashboard";
import { getResource } from "@/api/resource";
import { useAuthStore } from "@/stores/auth";
import { formatDate } from "@/utils/clinic";
import type { Pet, Visit } from "@/types";
const auth = useAuthStore();
const today = new Intl.DateTimeFormat("zh-CN", {
  year: "numeric",
  month: "long",
  day: "numeric",
  weekday: "long",
}).format(new Date());
const summary = ref<DashboardSummary>(),
  recent = ref<RecentVisit[]>([]),
  trend = ref<MetricPoint[]>([]),
  distribution = ref<MetricPoint[]>([]),
  workload = ref<MetricPoint[]>([]);
const photos = ref<Record<number, Pet>>({});
const loading = ref(false),
  errors = reactive({
    summary: false,
    recent: false,
    trend: false,
    distribution: false,
    workload: false,
  });
const stats: { key: keyof DashboardSummary; label: string; icon: string }[] = [
  { key: "scheduledVisits", label: "待就诊预约", icon: "calendar" },
  { key: "pets", label: "宠物档案", icon: "pet" },
  { key: "owners", label: "宠主档案", icon: "person" },
  { key: "vets", label: "执业兽医", icon: "doctor" },
];
const palette = ["#236b55", "#68a58b", "#82aeca", "#c59a63", "#9a90b4"];
const maxWorkload = computed(() =>
  Math.max(1, ...workload.value.map((item) => Number(item.value))),
);
const distributionTotal = computed(() =>
  distribution.value.reduce((sum, item) => sum + Number(item.value), 0),
);
const distributionDescription = computed(
  () =>
    distribution.value
      .map((item) => item.label + " " + item.value + "只")
      .join("，") || "暂无数据",
);
const trendDescription = computed(
  () =>
    trend.value
      .map((item) => item.label + " " + item.value + "个预约")
      .join("，") || "近七日无新建预约",
);
const trendRef = ref<HTMLElement>();
let chart: echarts.ECharts | undefined,
  observer: ResizeObserver | undefined,
  disposed = false;
const petForVisit = (id: number) => photos.value[id];
async function load() {
  loading.value = true;
  Object.keys(errors).forEach(
    (key) => (errors[key as keyof typeof errors] = false),
  );
  const results = await Promise.allSettled([
    getDashboardSummary(),
    getRecentVisits(),
    getVisitTrend(),
    getPetDistribution(),
    getVetWorkload(),
  ]);
  const keys = [
    "summary",
    "recent",
    "trend",
    "distribution",
    "workload",
  ] as const;
  results.forEach((result, index) => {
    if (result.status === "rejected") errors[keys[index]] = true;
  });
  if (disposed) return;
  if (results[0].status === "fulfilled")
    summary.value = results[0].value.data as DashboardSummary;
  if (results[1].status === "fulfilled")
    recent.value = results[1].value.data as RecentVisit[];
  if (results[2].status === "fulfilled")
    trend.value = results[2].value.data as MetricPoint[];
  if (results[3].status === "fulfilled")
    distribution.value = results[3].value.data as MetricPoint[];
  if (results[4].status === "fulfilled")
    workload.value = results[4].value.data as MetricPoint[];
  loading.value = false;
  await nextTick();
  if (trendRef.value && !errors.trend && !disposed) {
    chart ||= echarts.init(trendRef.value);
    const dates = Array.from({ length: 7 }, (_, index) => {
      const date = new Date();
      date.setDate(date.getDate() - 6 + index);
      return [
        date.getFullYear(),
        String(date.getMonth() + 1).padStart(2, "0"),
        String(date.getDate()).padStart(2, "0"),
      ].join("-");
    });
    chart.setOption({
      animation: !window.matchMedia("(prefers-reduced-motion: reduce)").matches,
      color: ["#236b55"],
      textStyle: { fontFamily: "Noto Sans SC, sans-serif" },
      tooltip: { trigger: "axis" },
      grid: { left: 32, right: 12, top: 16, bottom: 32 },
      xAxis: {
        type: "category",
        boundaryGap: false,
        data: dates.map(
          (date) => Number(date.slice(5, 7)) + "/" + Number(date.slice(8)),
        ),
        axisLine: { lineStyle: { color: "#dfe9e3" } },
        axisTick: { show: false },
        axisLabel: { color: "#667b72", fontSize: 12 },
      },
      yAxis: {
        type: "value",
        minInterval: 1,
        axisLabel: { color: "#667b72", fontSize: 12 },
        splitLine: { lineStyle: { color: "#eaf0ec", type: "dashed" } },
      },
      series: [
        {
          type: "line",
          smooth: false,
          symbolSize: 7,
          data: dates.map((date) =>
            Number(
              trend.value.find(
                (item) => String(item.label).slice(0, 10) === date,
              )?.value || 0,
            ),
          ),
          lineStyle: { width: 2 },
          areaStyle: { color: "rgba(35,107,85,.10)" },
        },
      ],
    });
    chart.resize();
  }
  if (auth.hasAuthority("visit:manage") && auth.hasAuthority("pet:manage")) {
    await Promise.allSettled(
      recent.value.map(async (visit) => {
        const result = await getResource<Visit>("/visits", visit.id);
        const pet = await getResource<Pet>("/pets", result.data.petId);
        if (!disposed) photos.value[visit.id] = pet.data;
      }),
    );
  }
}
onMounted(() => {
  load();
  if (trendRef.value) {
    observer = new ResizeObserver(() => chart?.resize());
    observer.observe(trendRef.value);
  }
});
onBeforeUnmount(() => {
  disposed = true;
  observer?.disconnect();
  chart?.dispose();
});
</script>
<style scoped>
.workbench-date {
  color: var(--clinic-muted);
  font-size: 14px;
  margin-right: 16px;
}
.summary-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  background: #fff;
  border: 1px solid var(--clinic-border);
  border-radius: 10px;
  padding: 16px;
  margin-bottom: 24px;
}
.summary-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 18px;
  border-right: 1px solid var(--clinic-border);
}
.summary-item:last-child {
  border: 0;
}
.summary-icon {
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: #edf5f0;
  color: var(--clinic-primary);
}
.summary-icon svg {
  width: 26px;
  height: 26px;
}
.summary-icon.amber {
  background: #fff4e5;
  color: #a96922;
}
.summary-label {
  display: block;
  color: var(--clinic-muted);
  font-size: 14px;
}
.summary-item strong {
  display: block;
  font-size: 40px;
  line-height: 1.2;
  color: var(--clinic-primary);
  margin-top: 5px;
  font-variant-numeric: tabular-nums;
}
.summary-item:first-child strong {
  color: #a96922;
}
.workbench-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.65fr) minmax(320px, 1fr);
  gap: 24px;
  margin-bottom: 24px;
}
.appointments-panel {
  align-self: stretch;
}
.analysis-column {
  min-width: 0;
}
.trend-chart {
  width: 100%;
  height: 150px;
}
.analysis-column > .panel {
  padding-block: 20px;
}
.patient-cell {
  display: flex;
  align-items: center;
  gap: 16px;
}
.patient-cell strong {
  font-size: 16px;
}
.appointment-table :deep(td.el-table__cell) {
  padding-block: 16px;
}
.workload-list {
  display: grid;
  gap: 8px;
}
.workload-row {
  display: grid;
  grid-template-columns: 70px minmax(0, 1fr) 28px;
  gap: 12px;
  align-items: center;
  font-size: 14px;
}
.workload-track {
  height: 18px;
  background: #eef3ef;
  border-radius: 3px;
  overflow: hidden;
}
.workload-track i {
  display: block;
  height: 100%;
  background: #4d9078;
  border-radius: 3px;
}
.workload-value {
  text-align: right;
  font-variant-numeric: tabular-nums;
}
.distribution-panel {
  display: grid;
  padding-block: 16px;
  grid-template-columns: 1fr 180px;
  gap: 16px 32px;
  align-items: center;
}
.distribution-panel .panel-head {
  grid-column: 1/-1;
  margin: 0;
}
.distribution-bar {
  height: 44px;
  display: flex;
  border-radius: 6px;
  overflow: hidden;
  background: #edf3ef;
}
.distribution-bar span {
  display: grid;
  place-items: center;
  min-width: 0;
  color: white;
  border-right: 2px solid white;
}
.distribution-bar b {
  font-size: 14px;
  font-weight: 500;
}
.distribution-legend {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 8px;
}
.distribution-legend li {
  display: grid;
  grid-template-columns: 10px 1fr auto;
  gap: 12px;
  align-items: center;
  font-size: 14px;
}
.distribution-legend i {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}
.distribution-legend strong {
  font-weight: 400;
  color: var(--clinic-muted);
}
@media (max-width: 1199px) {
  .workbench-grid {
    grid-template-columns: 1fr;
  }
  .analysis-column {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 24px;
  }
  .analysis-column .panel + .panel {
    margin: 0;
  }
}
@media (max-width: 639px) {
  .summary-strip {
    grid-template-columns: 1fr 1fr;
    padding: 16px 8px;
    gap: 20px 0;
  }
  .summary-item:nth-child(2) {
    border: 0;
  }
  .summary-item {
    gap: 12px;
    justify-content: flex-start;
    padding: 0 12px;
  }
  .summary-icon {
    width: 38px;
    height: 38px;
  }
  .summary-item strong {
    font-size: 28px;
  }
  .summary-label {
    font-size: 12px;
  }
  .analysis-column {
    grid-template-columns: 1fr;
  }
  .distribution-panel {
    grid-template-columns: 1fr;
  }
  .distribution-legend {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .workbench-date {
    font-size: 12px;
    margin-right: 0;
  }
}
</style>
