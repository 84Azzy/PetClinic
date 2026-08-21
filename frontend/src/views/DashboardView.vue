<template>
  <div>
    <div class="hero-card">
      <div>
        <span class="eyebrow teal">CLINIC OVERVIEW</span>
        <h2>早上好，{{ auth.user?.displayName || "诊所伙伴" }}</h2>
        <p>
          今天有
          <b>{{ summary.scheduledVisits }}</b>
          个预约等待接诊，祝你拥有高效又温暖的一天。
        </p>
      </div>
      <div class="hero-date">
        <b>{{ day }}</b
        ><span>{{ month }}月 · {{ week }}</span>
      </div>
    </div>
    <div class="stat-grid">
      <div v-for="item in stats" :key="item.label" class="stat-card">
        <div :class="['stat-icon', item.tone]">
          <el-icon><component :is="item.icon" /></el-icon>
        </div>
        <div>
          <span>{{ item.label }}</span
          ><strong>{{ item.value }}</strong
          ><small>{{ item.note }}</small>
        </div>
      </div>
    </div>
    <div class="chart-grid">
      <div class="panel">
        <div class="panel-head">
          <div>
            <h3>近七日预约趋势</h3>
            <p>按预约创建时间统计</p>
          </div>
          <el-tag type="success" effect="light">实时数据</el-tag>
        </div>
        <div ref="trendRef" class="chart"></div>
      </div>
      <div class="panel">
        <div class="panel-head">
          <div>
            <h3>宠物类型分布</h3>
            <p>当前有效宠物档案</p>
          </div>
        </div>
        <div ref="petRef" class="chart"></div>
      </div>
    </div>
    <div class="panel">
      <div class="panel-head">
        <div>
          <h3>近期预约</h3>
          <p>快速掌握最新接诊安排</p>
        </div>
        <el-button type="primary" plain @click="$router.push('/visits')"
          >查看全部</el-button
        >
      </div>
      <el-table :data="recent" stripe
        ><el-table-column prop="petName" label="宠物" /><el-table-column
          prop="vetName"
          label="接诊兽医"
        /><el-table-column prop="startTime" label="预约时间" /><el-table-column
          prop="status"
          label="状态"
          ><template #default="scope"
            ><el-tag
              :type="scope.row.status === 'COMPLETED' ? 'success' : 'warning'"
              >{{
                scope.row.status === "COMPLETED" ? "已完成" : "待就诊"
              }}</el-tag
            ></template
          ></el-table-column
        ></el-table
      >
    </div>
  </div>
</template>
<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from "vue";
import * as echarts from "echarts";
import { useAuthStore } from "@/stores/auth";
import http from "@/api/http";
const auth = useAuthStore();
const now = new Date();
const day = now.getDate();
const month = now.getMonth() + 1;
const week = [
  "星期日",
  "星期一",
  "星期二",
  "星期三",
  "星期四",
  "星期五",
  "星期六",
][now.getDay()];
const summary = ref({ owners: 2, pets: 4, vets: 4, scheduledVisits: 1 });
const recent = ref([
  {
    petName: "元宝",
    vetName: "陈医生",
    startTime: "2026-08-22 14:00",
    status: "SCHEDULED",
  },
  {
    petName: "糯米",
    vetName: "王医生",
    startTime: "2026-08-22 09:00",
    status: "COMPLETED",
  },
]);
const stats = computed(() => [
  {
    label: "宠主档案",
    value: summary.value.owners,
    note: "位活跃宠主",
    icon: "User",
    tone: "blue",
  },
  {
    label: "宠物档案",
    value: summary.value.pets,
    note: "只健康伙伴",
    icon: "MostlyCloudy",
    tone: "teal",
  },
  {
    label: "执业兽医",
    value: summary.value.vets,
    note: "位接诊医生",
    icon: "Avatar",
    tone: "violet",
  },
  {
    label: "待就诊预约",
    value: summary.value.scheduledVisits,
    note: "项日程待处理",
    icon: "Calendar",
    tone: "orange",
  },
]);
const trendRef = ref<HTMLElement>();
const petRef = ref<HTMLElement>();
onMounted(async () => {
  try {
    summary.value = (await http.get<any, any>("/dashboard/summary")).data;
    recent.value = (await http.get<any, any>("/dashboard/recent-visits")).data;
  } catch {}
  await nextTick();
  echarts.init(trendRef.value!).setOption({
    grid: { left: 35, right: 20, top: 25, bottom: 28 },
    xAxis: {
      type: "category",
      data: ["8/16", "8/17", "8/18", "8/19", "8/20", "8/21", "8/22"],
      axisLine: { lineStyle: { color: "#dfe5ec" } },
    },
    yAxis: { type: "value", splitLine: { lineStyle: { color: "#eef2f6" } } },
    series: [
      {
        type: "line",
        smooth: true,
        data: [4, 7, 5, 9, 8, 12, 10],
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: "rgba(0,150,136,.28)" },
            { offset: 1, color: "rgba(0,150,136,.02)" },
          ]),
        },
        lineStyle: { color: "#009688", width: 3 },
        itemStyle: { color: "#009688" },
      },
    ],
  });
  echarts.init(petRef.value!).setOption({
    tooltip: { trigger: "item" },
    legend: { bottom: 0 },
    series: [
      {
        type: "pie",
        radius: ["48%", "72%"],
        center: ["50%", "44%"],
        data: [
          { name: "猫", value: 2 },
          { name: "犬", value: 1 },
          { name: "兔", value: 1 },
        ],
        itemStyle: { borderColor: "#fff", borderWidth: 4 },
        label: { formatter: "{b}\n{d}%" },
      },
    ],
  });
});
</script>
