<template>
  <div class="ai-page">
    <PageHeader
      title="AI 诊疗助手"
      description="查询诊疗安排，整理预约草稿，每一步都由你确认。"
      ><el-button
        class="mobile-history-button"
        @click="historyOpen = !historyOpen"
        >{{ historyOpen ? "收起历史" : "历史会话" }}</el-button
      ></PageHeader
    >
    <div class="ai-shell">
      <aside>
        <div class="new-chat">
          <el-button
            type="primary"
            :icon="Plus"
            :disabled="sending || !!confirmingDraftKey"
            @click="newChat"
            >新对话</el-button
          >
        </div>
        <div v-loading="loadingConversations" class="conversation-list">
          <PageState
            v-if="conversationsError"
            kind="error"
            title="会话加载失败"
            @retry="loadConversations"
          />
          <div
            v-for="c in conversations"
            :key="c.id"
            :class="['conversation', { active: c.id === conversationId }]"
            role="group"
          >
            <button
              class="conversation-select"
              :aria-pressed="c.id === conversationId"
              :disabled="sending || !!confirmingDraftKey"
              @click="selectConversation(c.id)"
            >
              <ClinicIcon name="chat" /><span>{{ c.title }}</span>
            </button>
            <el-button
              text
              circle
              :icon="Delete"
              :aria-label="'删除会话：' + c.title"
              :disabled="sending || !!confirmingDraftKey"
              @click.stop="removeConversation(c)"
            />
          </div>
          <el-empty
            v-if="
              !loadingConversations &&
              !conversationsError &&
              conversations.length === 0
            "
            description="暂无历史会话"
            :image-size="64"
          />
        </div>
      </aside>
      <section class="chat">
        <div class="chat-head">
          <div class="ai-avatar">
            <el-icon><MagicStick /></el-icon>
          </div>
          <div>
            <b>宠安 AI 助手</b><span>业务查询 · 预约草稿 · 确认后创建</span>
          </div>
          <el-tag
            :type="
              connection === '响应正常'
                ? 'success'
                : connection === '连接异常'
                  ? 'danger'
                  : 'info'
            "
            >{{ sending ? "正在请求" : connection }}</el-tag
          >
        </div>
        <div
          ref="messagesRef"
          v-loading="loadingMessages"
          class="messages"
          role="log"
          aria-label="会话消息"
          aria-live="polite"
          aria-relevant="additions"
        >
          <PageState
            v-if="messagesError"
            kind="error"
            title="消息加载失败"
            @retry="conversationId && selectConversation(conversationId)"
          />
          <div
            v-if="!loadingMessages && !messagesError && messages.length === 0"
            class="welcome"
          >
            <div class="ai-avatar large">
              <el-icon><MagicStick /></el-icon>
            </div>
            <h3>你好，我是宠安 AI 助手</h3>
            <p>
              我可以帮你查询宠物、兽医可用时段和预约，并在你确认草稿后创建预约。
            </p>
            <div class="prompts">
              <button v-for="p in prompts" :key="p" @click="input = p">
                {{ p }}
              </button>
            </div>
          </div>
          <div
            v-for="(m, i) in messages"
            :key="m.id ?? `${m.role}-${i}`"
            :class="['message-row', m.role]"
          >
            <div v-if="m.role === 'tool'" class="tool-message">
              <el-icon><Connection /></el-icon>
              {{ toolLabel(m.toolName) }}
            </div>
            <template v-else>
              <div :class="['message', { failure: m.failure }]">
                {{ m.content }}
              </div>
              <div v-if="m.draft" class="draft-card">
                <b>预约草稿</b>
                <span
                  >就诊宠物：{{
                    draftPetNames[m.draft.petId] || "宠物 #" + m.draft.petId
                  }}</span
                >
                <span
                  >预约时段：{{
                    draftSlotLabels[m.draft.slotId] || "时段 #" + m.draft.slotId
                  }}</span
                >
                <span>就诊原因：{{ m.draft.reason }}</span>
                <p v-if="m.draft.summary">{{ m.draft.summary }}</p>
                <template v-if="m.createdVisitId">
                  <el-tag type="success" effect="plain">
                    已创建预约 #{{ m.createdVisitId }}
                  </el-tag>
                  <StatusTag
                    v-if="m.createdVisitStatus"
                    :status="m.createdVisitStatus"
                  />
                  <el-button
                    v-if="auth.canVisitPath('/visits')"
                    plain
                    @click="$router.push('/visits?visitId=' + m.createdVisitId)"
                    >查看预约</el-button
                  >
                </template>
                <template v-else>
                  <el-tag :type="m.draftError ? 'danger' : 'warning'">{{
                    m.draftError ? "创建失败，可重试" : "尚未创建，等待确认"
                  }}</el-tag>
                  <p v-if="m.draftError" role="alert">{{ m.draftError }}</p>
                  <el-button
                    v-if="canCreateVisit"
                    type="primary"
                    :icon="CircleCheck"
                    :loading="confirmingDraftKey === draftKey(m.draft)"
                    :disabled="!!confirmingDraftKey || sending"
                    @click="confirmDraft(m)"
                    >确认并创建预约</el-button
                  >
                </template>
              </div>
            </template>
          </div>
          <div v-if="sending" class="message-row assistant">
            <div class="message thinking">正在查询并整理结果…</div>
          </div>
        </div>
        <div class="composer">
          <el-input
            v-model="input"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 4 }"
            placeholder="例如：给我的猫找明天下午的外科医生"
            aria-label="给诊疗助手发送消息"
            :disabled="sending"
            @keydown.ctrl.enter.prevent="send"
          /><el-button
            type="primary"
            circle
            :icon="Promotion"
            aria-label="发送消息"
            :loading="sending"
            :disabled="!input.trim() || sending"
            @click="send"
          /><small
            >Ctrl + Enter 发送 · AI
            生成草稿后，请点击卡片中的确认按钮创建预约</small
          >
        </div>
      </section>
    </div>
    <el-drawer
      v-model="historyOpen"
      title="历史会话"
      direction="ltr"
      size="300px"
      append-to-body
    >
      <el-button
        type="primary"
        :icon="Plus"
        :disabled="sending || !!confirmingDraftKey"
        style="width: 100%; margin-bottom: 20px"
        @click="newChat"
        >新对话</el-button
      >
      <PageState v-if="loadingConversations" kind="loading" /><PageState
        v-else-if="conversationsError"
        kind="error"
        title="会话加载失败"
        @retry="loadConversations"
      /><PageState
        v-else-if="!conversations.length"
        kind="empty"
        title="暂无历史会话"
        description="发送消息后，会话会显示在这里。"
      />
      <div
        v-for="c in conversations"
        v-else
        :key="c.id"
        :class="['conversation', { active: c.id === conversationId }]"
      >
        <button
          class="conversation-select"
          :aria-pressed="c.id === conversationId"
          :disabled="sending || !!confirmingDraftKey"
          @click="selectConversation(c.id)"
        >
          <ClinicIcon name="chat" /><span>{{ c.title }}</span></button
        ><el-button
          text
          circle
          :icon="Delete"
          :aria-label="'删除会话：' + c.title"
          :disabled="sending || !!confirmingDraftKey"
          @click="removeConversation(c)"
        />
      </div>
    </el-drawer>
  </div>
</template>
<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from "vue";
import {
  CircleCheck,
  Connection,
  Delete,
  Plus,
  Promotion,
  MagicStick,
} from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";
import PageHeader from "@/components/PageHeader.vue";
import PageState from "@/components/PageState.vue";
import ClinicIcon from "@/components/ClinicIcon.vue";
import StatusTag from "@/components/StatusTag.vue";
import { getResource, listResource } from "@/api/resource";
import { formatDate, type ClinicRow } from "@/utils/clinic";
import {
  deleteAiConversation,
  listAiConversations,
  listAiMessages,
  sendAiMessage,
  type AiConversation,
  type AppointmentDraft,
} from "@/api/ai";
import { createVisit } from "@/api/visits";
import { useAuthStore } from "@/stores/auth";

type UiMessage = {
  id?: number;
  role: "user" | "assistant" | "tool";
  content: string;
  toolName?: string;
  draft?: AppointmentDraft;
  createdVisitId?: number;
  createdVisitStatus?: string;
  failure?: boolean;
  draftError?: string;
};

const auth = useAuthStore();
const conversationId = ref<number>();
const conversations = ref<AiConversation[]>([]);
const messages = ref<UiMessage[]>([]);
const input = ref("");
const loadingConversations = ref(false);
const loadingMessages = ref(false);
const sending = ref(false);
const confirmingDraftKey = ref("");
const connection = ref("连接待验证"),
  conversationsError = ref(false),
  messagesError = ref(false),
  historyOpen = ref(false),
  messagesRef = ref<HTMLElement>();
const draftPetNames = ref<Record<number, string>>({}),
  draftSlotLabels = ref<Record<number, string>>({});
async function loadDraftLabels(draft?: AppointmentDraft) {
  if (!draft) return;
  await Promise.allSettled([
    auth.hasAuthority("pet:manage")
      ? getResource<ClinicRow>("/pets", draft.petId).then(
          (r) => (draftPetNames.value[draft.petId] = r.data.name),
        )
      : Promise.resolve(),
    getResource<ClinicRow>("/slots", draft.slotId).then(
      (r) =>
        (draftSlotLabels.value[draft.slotId] =
          formatDate(r.data.startTime, true) +
          " – " +
          r.data.endTime.slice(11, 16)),
    ),
  ]);
}
watch([() => messages.value.length, sending], async () => {
  await nextTick();
  if (messagesRef.value)
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight;
});
const prompts = ["查询我的宠物", "查找明天下午可用兽医", "查看我的预约"];
const toolLabel = (name?: string) =>
  ({
    listMyPets: "已查询宠物档案",
    findVets: "已查询接诊兽医",
    findAvailableAppointments: "已查询可预约安排",
    findAvailableSlots: "已查询可用时段",
    listMyVisits: "已查询预约记录",
    buildAppointmentDraft: "已整理预约草稿",
  })[name || ""] || "业务查询已完成";
const canCreateVisit = computed(
  () => auth.hasRole("OWNER") && auth.hasAuthority("visit:create"),
);

const draftKey = (draft: AppointmentDraft) =>
  `${conversationId.value ?? "new"}-${draft.petId}-${draft.slotId}`;

async function loadDraftState(message: UiMessage) {
  if (!message.draft || !auth.hasAuthority("visit:manage")) return;
  const requestId = "ai-" + draftKey(message.draft);
  const response = await listResource<ClinicRow>("/visits", {
    page: 1,
    size: 10,
    keyword: requestId,
  });
  const records = Array.isArray(response.data)
    ? response.data
    : response.data.records;
  const visit = records.find((row) => row.requestId === requestId);
  if (visit) {
    message.createdVisitId = visit.id;
    message.createdVisitStatus = visit.status;
  }
}

/*
 * TODO【新知识：把 AI 建议与真实写操作分开】
 * draft 只是普通对象，点击按钮后才调用 createVisit。后端仍会重新校验宠物归属并原子抢占时段，
 * 所以不能把“前端已有草稿”当成预约已经成功。
 */
const confirmDraft = async (message: UiMessage) => {
  const draft = message.draft;
  if (
    !draft ||
    confirmingDraftKey.value ||
    message.createdVisitId ||
    !canCreateVisit.value
  )
    return;

  try {
    await ElMessageBox.confirm(
      draft.summary ||
        `确认使用宠物 #${draft.petId} 预约时段 #${draft.slotId}，就诊原因：${draft.reason}？`,
      "确认创建预约",
      {
        type: "warning",
        confirmButtonText: "确认预约",
        cancelButtonText: "再想想",
      },
    );
  } catch {
    return;
  }

  const key = draftKey(draft);
  confirmingDraftKey.value = key;
  try {
    /*
     * TODO【新知识：幂等键】
     * 同一份 AI 草稿始终生成相同 requestId。即使网络超时后用户再次点击，后端也只会创建一次。
     */
    const requestId = `ai-${key}`;
    const response = await createVisit({
      petId: draft.petId,
      slotId: draft.slotId,
      reason: draft.reason,
      requestId,
    });
    message.createdVisitId = response.data.id;
    message.createdVisitStatus = response.data.status;
    message.draftError = undefined;
    ElMessage.success(`预约创建成功，预约编号 #${response.data.id}`);
  } catch (error) {
    message.draftError =
      (error as { response?: { data?: { message?: string } } }).response?.data
        ?.message || "创建预约失败，请确认时段仍可用后重试。";
  } finally {
    confirmingDraftKey.value = "";
  }
};

const parseDraft = (payload?: string): AppointmentDraft | undefined => {
  if (!payload) return undefined;
  try {
    const value = JSON.parse(payload) as Partial<AppointmentDraft>;
    if (value.petId && value.slotId && value.reason) {
      return value as AppointmentDraft;
    }
  } catch {
    // 历史审计数据格式异常时只忽略草稿卡片，不影响消息正文展示。
  }
  return undefined;
};

const loadConversations = async () => {
  loadingConversations.value = true;
  conversationsError.value = false;
  try {
    conversations.value = (await listAiConversations()).data;
  } catch {
    conversationsError.value = true;
  } finally {
    loadingConversations.value = false;
  }
};

const selectConversation = async (id: number) => {
  if (sending.value || confirmingDraftKey.value) return;
  conversationId.value = id;
  historyOpen.value = false;
  messagesError.value = false;
  messages.value = [];
  loadingMessages.value = true;
  try {
    const history = (await listAiMessages(id)).data;
    if (conversationId.value !== id) return;
    messages.value = history.map((message) => ({
      id: message.id,
      role: message.role.toLowerCase() as UiMessage["role"],
      content: message.content,
      toolName: message.toolName,
      draft:
        message.role === "ASSISTANT"
          ? parseDraft(message.toolPayload)
          : undefined,
    }));
    await Promise.allSettled(
      messages.value.flatMap((m) => [
        loadDraftLabels(m.draft),
        loadDraftState(m),
      ]),
    );
  } catch {
    if (conversationId.value === id) messagesError.value = true;
  } finally {
    if (conversationId.value === id) loadingMessages.value = false;
  }
};

const newChat = () => {
  if (sending.value || confirmingDraftKey.value) return;
  conversationId.value = undefined;
  messages.value = [];
  loadingMessages.value = false;
  messagesError.value = false;
  historyOpen.value = false;
};

const removeConversation = async (conversation: AiConversation) => {
  try {
    await ElMessageBox.confirm(
      `确定删除会话“${conversation.title}”吗？`,
      "删除会话",
      { type: "warning", confirmButtonText: "删除", cancelButtonText: "取消" },
    );
  } catch {
    return;
  }
  try {
    await deleteAiConversation(conversation.id);
    const removedCurrent = conversationId.value === conversation.id;
    await loadConversations();
    if (removedCurrent) newChat();
    ElMessage.success("会话已删除");
  } catch {}
};

const send = async () => {
  const content = input.value.trim();
  if (
    !content ||
    sending.value ||
    confirmingDraftKey.value ||
    loadingMessages.value
  )
    return;
  const userMessage: UiMessage = { role: "user", content };
  messages.value.push(userMessage);
  input.value = "";
  sending.value = true;
  try {
    const response = (
      await sendAiMessage({
        conversationId: conversationId.value,
        message: content,
      })
    ).data;
    conversationId.value = response.conversationId;
    connection.value = "响应正常";
    messages.value.push(
      ...response.toolsUsed.map<UiMessage>((toolName) => ({
        role: "tool",
        content: "工具调用成功",
        toolName,
      })),
      {
        role: "assistant",
        content: response.answer,
        draft: response.draft,
      },
    );
    await loadDraftLabels(response.draft);
    await loadConversations();
  } catch (error) {
    connection.value = "连接异常";
    const responseMessage = (
      error as { response?: { data?: { message?: string } } }
    ).response?.data?.message;
    messages.value.push({
      role: "assistant",
      content: `本次请求失败：${responseMessage || "AI 服务暂时没有返回有效内容，请重试。"}`,
      failure: true,
    });
    input.value = content;
  } finally {
    sending.value = false;
  }
};

onMounted(async () => {
  await loadConversations();
  if (conversations.value[0]) {
    await selectConversation(conversations.value[0].id);
  }
});
</script>
<style scoped>
.ai-page {
  height: 100%;
  min-height: 560px;
  display: flex;
  flex-direction: column;
}
.ai-shell {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  background: #fff;
  border: 1px solid var(--clinic-border);
  border-radius: 10px;
  overflow: hidden;
}
.ai-shell > aside {
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #f8faf8;
  border-right: 1px solid var(--clinic-border);
  padding: 16px;
}
.new-chat .el-button {
  width: 100%;
  margin-bottom: 16px;
}
.conversation-list {
  flex: 1;
  min-height: 120px;
  overflow-y: auto;
}
.conversation {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px;
  border-radius: 6px;
  color: var(--clinic-muted);
  margin-bottom: 4px;
}
.conversation-select {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
  color: inherit;
  border: 0;
  padding: 12px 8px;
  background: none;
  text-align: left;
  cursor: pointer;
}
.conversation-select svg {
  width: 18px;
  flex: none;
}
.conversation-select span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.conversation .el-button {
  color: inherit;
}
.conversation.active,
.conversation:hover {
  background: #eaf4ee;
  color: var(--clinic-primary);
}
.conversation button:disabled {
  cursor: wait;
  opacity: 0.65;
}
.chat {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}
.chat-head {
  flex-shrink: 0;
  min-height: 80px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 24px;
  border-bottom: 1px solid var(--clinic-border);
}
.chat-head > div:nth-child(2) {
  flex: 1;
}
.chat-head b {
  display: block;
  font-size: 16px;
}
.chat-head div > span {
  display: block;
  font-size: 12px;
  color: var(--clinic-muted);
  margin-top: 4px;
}
.ai-avatar {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  flex: none;
  border-radius: 6px;
  color: var(--clinic-primary);
  background: #eaf4ee;
}
.ai-avatar.large {
  width: 56px;
  height: 56px;
  margin: auto;
  font-size: 24px;
}
.messages {
  flex: 1;
  min-height: 260px;
  overflow: auto;
  padding: 24px;
}
.welcome {
  text-align: center;
  max-width: 620px;
  margin: 48px auto;
}
.welcome h3 {
  margin: 20px 0 12px;
  font-size: 20px;
}
.welcome p {
  color: var(--clinic-muted);
  line-height: 1.9;
}
.prompts {
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 24px;
}
.prompts button {
  border: 1px solid var(--clinic-border);
  border-radius: 6px;
  background: #fff;
  color: var(--clinic-primary);
  padding: 12px 16px;
  cursor: pointer;
}
.prompts button:hover {
  border-color: var(--clinic-primary);
  background: var(--clinic-canvas);
}
.message-row {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin: 16px 0;
}
.message-row.user {
  align-items: flex-end;
}
.message-row .message {
  max-width: 85%;
  padding: 16px;
  border-radius: 10px;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  line-height: 1.85;
}
.message-row.user .message {
  background: var(--clinic-primary);
  color: #fff;
}
.message-row.assistant .message {
  background: #f3f7f5;
  color: var(--clinic-ink);
}
.message-row .message.failure {
  background: #fcf0f0;
  color: #a34444;
}
.message.thinking {
  color: var(--clinic-muted);
}
.tool-message {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 8px;
  color: var(--clinic-muted);
  font-size: 12px;
}
.draft-card {
  display: grid;
  gap: 12px;
  width: min(480px, 100%);
  margin-top: 12px;
  padding: 20px;
  border: 1px solid #c8ddd0;
  border-radius: 10px;
  background: #fbfdfb;
  color: var(--clinic-ink);
  font-size: 14px;
  overflow-wrap: anywhere;
}
.draft-card b {
  color: var(--clinic-primary);
  font-size: 16px;
}
.draft-card p {
  margin: 0;
  white-space: pre-wrap;
}
.draft-card .el-tag {
  width: fit-content;
}
.draft-card .el-button {
  margin: 0;
}
.composer {
  flex-shrink: 0;
  position: relative;
  padding: 16px 76px 48px 24px;
  border-top: 1px solid var(--clinic-border);
  background: #fff;
}
.composer > .el-button {
  position: absolute;
  right: 24px;
  top: 24px;
}
.composer small {
  position: absolute;
  left: 24px;
  right: 24px;
  bottom: 12px;
  color: var(--clinic-muted);
  font-size: 12px;
}
.mobile-history-button {
  display: none;
}
@media (max-width: 767px) {
  .ai-page {
    min-height: 640px;
  }
  .ai-shell {
    grid-template-columns: 1fr;
    grid-template-rows: minmax(0, 1fr);
  }
  .ai-shell > aside {
    display: none;
    max-height: 240px;
    border-right: 0;
    border-bottom: 1px solid var(--clinic-border);
  }
  .ai-shell > aside.mobile-visible {
    display: flex;
  }
  .ai-shell:has(aside.mobile-visible) {
    grid-template-rows: auto minmax(0, 1fr);
  }
  .mobile-history-button {
    display: inline-flex;
  }
  .chat-head {
    padding: 16px;
    flex-wrap: wrap;
  }
  .chat-head > .el-tag {
    margin-left: 52px;
  }
  .messages {
    padding: 16px;
  }
  .message-row .message {
    max-width: 95%;
  }
  .composer {
    padding: 16px 64px 64px 16px;
  }
  .composer > .el-button {
    right: 16px;
  }
  .composer small {
    left: 16px;
    right: 16px;
    bottom: 12px;
    font-size: 12px;
  }
  .welcome {
    margin: 24px auto;
  }
}
</style>
