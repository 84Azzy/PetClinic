<template>
  <div>
    <PageHeader
      title="操作日志"
      description="查看操作结果，追踪异常与处理耗时。"
    />
    <section class="panel">
      <div class="panel-head">
        <h2>最近业务写操作</h2>
        <el-button @click="load">刷新</el-button>
      </div>
      <PageState v-if="error" kind="error" @retry="load" /><template v-else
        ><el-table v-loading="loading" :data="rows" class="desktop-table"
          ><el-table-column label="结果" width="100"
            ><template #default="{ row }"
              ><el-tag
                :type="row.responseStatus < 400 ? 'success' : 'danger'"
                >{{ row.responseStatus < 400 ? "成功" : "失败" }}</el-tag
              ></template
            ></el-table-column
          ><el-table-column prop="createdAt" label="操作时间" min-width="170"
            ><template #default="{ row }">{{
              formatDate(row.createdAt, true)
            }}</template></el-table-column
          ><el-table-column
            prop="username"
            label="操作人"
            min-width="120" /><el-table-column
            prop="module"
            label="模块"
            min-width="120" /><el-table-column
            prop="operation"
            label="操作"
            min-width="180"
            show-overflow-tooltip /><el-table-column label="耗时" width="110"
            ><template #default="{ row }"
              >{{ row.durationMs }} ms</template
            ></el-table-column
          ><el-table-column label="详情" width="90" fixed="right"
            ><template #default="{ row }"
              ><el-button link type="primary" @click="detail(row)"
                >查看</el-button
              ></template
            ></el-table-column
          ><template #empty
            ><PageState
              kind="empty"
              title="暂无操作日志"
              description="业务操作完成后，记录会显示在这里。" /></template
        ></el-table>
        <div class="mobile-records">
          <article v-for="row in rows" :key="row.id" class="mobile-record-card">
            <div class="panel-head">
              <strong>{{ row.operation }}</strong
              ><el-tag
                :type="row.responseStatus < 400 ? 'success' : 'danger'"
                >{{ row.responseStatus < 400 ? "成功" : "失败" }}</el-tag
              >
            </div>
            <p>{{ row.username }} · {{ row.module }}</p>
            <p class="caption">
              {{ formatDate(row.createdAt, true) }} · {{ row.durationMs }} ms
            </p>
            <el-button type="primary" plain @click="detail(row)"
              >查看详情</el-button
            >
          </article>
          <PageState
            v-if="!loading && !rows.length"
            kind="empty"
            title="暂无日志"
          />
        </div>
        <div class="pager">
          <el-pagination
            v-model:current-page="query.page"
            v-model:page-size="query.size"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="load"
            @size-change="search"
          /></div
      ></template>
    </section>
    <el-drawer v-model="visible" title="操作详情" size="620px"
      ><template v-if="selected"
        ><el-tag :type="selected.responseStatus < 400 ? 'success' : 'danger'"
          >{{ selected.responseStatus < 400 ? "操作成功" : "操作失败" }} · HTTP
          {{ selected.responseStatus }}</el-tag
        >
        <h2 style="margin-top: 24px">{{ selected.operation }}</h2>
        <dl class="detail-grid">
          <div
            v-for="field in detailFields"
            :key="field.key"
            :class="['detail-item', { wide: field.key === 'requestUri' }]"
          >
            <dt>{{ field.label }}</dt>
            <dd>{{ selected[field.key] ?? "未记录" }}</dd>
          </div>
        </dl>
        <section class="detail-section">
          <h3>异常详情</h3>
          <pre v-if="selected.errorMessage" class="log-error">{{
            selected.errorMessage
          }}</pre>
          <p v-else class="muted">此操作未记录异常。</p>
        </section></template
      ></el-drawer
    >
  </div>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { Search } from "@element-plus/icons-vue";
import PageHeader from "@/components/PageHeader.vue";
import PageState from "@/components/PageState.vue";
import { listResource } from "@/api/resource";
import { formatDate, type ClinicRow } from "@/utils/clinic";
const rows = ref<ClinicRow[]>([]),
  selected = ref<ClinicRow>(),
  loading = ref(false),
  error = ref(false),
  visible = ref(false),
  total = ref(0),
  query = reactive({ page: 1, size: 10, keyword: "" });
const detailFields = [
  { key: "username", label: "操作账号" },
  { key: "createdAt", label: "操作时间" },
  { key: "module", label: "操作模块" },
  { key: "durationMs", label: "耗时（毫秒）" },
  { key: "httpMethod", label: "请求方法" },
  { key: "ipAddress", label: "来源地址" },
  { key: "requestUri", label: "请求路径" },
];
let generation = 0;
async function load() {
  const current = ++generation;
  loading.value = true;
  error.value = false;
  try {
    const r = await listResource<ClinicRow>("/operation-logs", {
      ...query,
      keyword: query.keyword.trim() || undefined,
    });
    if (current !== generation) return;
    rows.value = Array.isArray(r.data) ? r.data : r.data.records;
    total.value = Array.isArray(r.data) ? r.data.length : r.data.total;
  } catch {
    if (current === generation) error.value = true;
  } finally {
    if (current === generation) loading.value = false;
  }
}
function search() {
  query.page = 1;
  load();
}
function detail(row: ClinicRow) {
  selected.value = row;
  visible.value = true;
}
onMounted(load);
</script>
<style scoped>
.log-error {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  background: #fcf0f0;
  color: #a34444;
  border-radius: 6px;
  padding: 16px;
  font-size: 14px;
}
</style>
