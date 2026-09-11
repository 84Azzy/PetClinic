<template>
  <div class="ai-page">
    <PageHeader
      title="AI 诊疗助手"
      description="自然语言查询宠物、兽医时段和预约，并生成可确认的预约草稿"
    />
    <div class="ai-shell">
      <aside>
        <div class="new-chat">
          <el-button type="primary" :icon="Plus" @click="newChat"
            >新对话</el-button
          >
        </div>
        <div v-loading="loadingConversations" class="conversation-list">
          <div
            v-for="c in conversations"
            :key="c.id"
            :class="['conversation', { active: c.id === conversationId }]"
            @click="selectConversation(c.id)"
          >
            <el-icon><ChatDotRound /></el-icon><span>{{ c.title }}</span>
            <el-button
              text
              circle
              :icon="Delete"
              title="删除会话"
              @click.stop="removeConversation(c)"
            />
          </div>
          <el-empty
            v-if="!loadingConversations && conversations.length === 0"
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
            <b>宠安 AI 助手</b><span>业务查询 · 预约草稿 · 不直接写库</span>
          </div>
          <el-tag type="success" effect="plain">DeepSeek 已接通</el-tag>
        </div>
        <div v-loading="loadingMessages" class="messages">
          <div v-if="!loadingMessages && messages.length === 0" class="welcome">
            <div class="ai-avatar large">
              <el-icon><MagicStick /></el-icon>
            </div>
            <h3>你好，我是宠安 AI 助手</h3>
            <p>
              我可以帮你查询宠物、兽医可用时段和预约，并生成待确认的预约草稿。
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
              已调用业务工具：{{ m.toolName }}
            </div>
            <template v-else>
              <div class="message">{{ m.content }}</div>
              <div v-if="m.draft" class="draft-card">
                <b>预约草稿</b>
                <span>宠物 ID：{{ m.draft.petId }}</span>
                <span>时段 ID：{{ m.draft.slotId }}</span>
                <span>就诊原因：{{ m.draft.reason }}</span>
                <p v-if="m.draft.summary">{{ m.draft.summary }}</p>
                <el-tag type="warning" effect="plain">尚未创建，等待确认</el-tag>
              </div>
            </template>
          </div>
          <div v-if="sending" class="message-row assistant">
            <div class="message thinking">DeepSeek 正在查询并整理结果…</div>
          </div>
        </div>
        <div class="composer">
          <el-input
            v-model="input"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 4 }"
            placeholder="例如：给我的猫找明天下午的外科医生"
            :disabled="sending"
            @keydown.ctrl.enter.prevent="send"
          /><el-button
            type="primary"
            circle
            :icon="Promotion"
            :loading="sending"
            :disabled="!input.trim() || sending"
            @click="send"
          /><small
            >Ctrl + Enter 发送 · AI
            只生成草稿，最终预约由普通业务接口确认</small
          >
        </div>
      </section>
    </div>
  </div>
</template>
<script setup lang="ts">
import { onMounted, ref } from "vue";
import { Connection, Delete, Plus, Promotion } from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";
import PageHeader from "@/components/PageHeader.vue";
import {
  deleteAiConversation,
  listAiConversations,
  listAiMessages,
  sendAiMessage,
  type AiConversation,
  type AppointmentDraft,
} from "@/api/ai";

type UiMessage = {
  id?: number;
  role: "user" | "assistant" | "tool";
  content: string;
  toolName?: string;
  draft?: AppointmentDraft;
};

const conversationId = ref<number>();
const conversations = ref<AiConversation[]>([]);
const messages = ref<UiMessage[]>([]);
const input = ref("");
const loadingConversations = ref(false);
const loadingMessages = ref(false);
const sending = ref(false);
const prompts = ["查询我的宠物", "查找明天下午可用兽医", "查看我的预约"];

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
  try {
    conversations.value = (await listAiConversations()).data;
  } finally {
    loadingConversations.value = false;
  }
};

const selectConversation = async (id: number) => {
  conversationId.value = id;
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
  } finally {
    if (conversationId.value === id) loadingMessages.value = false;
  }
};

const newChat = () => {
  conversationId.value = undefined;
  messages.value = [];
  loadingMessages.value = false;
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
  await deleteAiConversation(conversation.id);
  const removedCurrent = conversationId.value === conversation.id;
  await loadConversations();
  if (removedCurrent) newChat();
  ElMessage.success("会话已删除");
};

const send = async () => {
  const content = input.value.trim();
  if (!content || sending.value) return;
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
    await loadConversations();
  } catch (error) {
    const index = messages.value.indexOf(userMessage);
    if (index >= 0) messages.value.splice(index, 1);
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
.ai-shell {
  height: calc(100vh - 190px);
  min-height: 560px;
  display: grid;
  grid-template-columns: 240px 1fr;
  background: #fff;
  border: 1px solid #e2e8ed;
  border-radius: 14px;
  overflow: hidden;
}
.ai-shell > aside {
  background: #f7faf9;
  border-right: 1px solid #e3e9e7;
  padding: 14px;
}
.new-chat .el-button {
  width: 100%;
  margin-bottom: 14px;
}
.conversation-list {
  min-height: 120px;
}
.conversation {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 11px;
  border-radius: 8px;
  color: #65747e;
  font-size: 13px;
  cursor: pointer;
}
.conversation span {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.conversation .el-button {
  opacity: 0;
  color: inherit;
}
.conversation:hover .el-button,
.conversation.active .el-button {
  opacity: 1;
}
.conversation.active,
.conversation:hover {
  background: #e5f6f3;
  color: #007f73;
}
.chat {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.chat-head {
  height: 68px;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 0 20px;
  border-bottom: 1px solid #edf0f2;
}
.chat-head div:nth-child(2) {
  flex: 1;
}
.chat-head b,
.chat-head span {
  display: block;
}
.chat-head span {
  font-size: 11px;
  color: #93a0aa;
  margin-top: 3px;
}
.ai-avatar {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 12px;
  color: #fff;
  background: linear-gradient(135deg, #00bfae, #007f73);
}
.ai-avatar.large {
  width: 54px;
  height: 54px;
  margin: auto;
  font-size: 24px;
}
.messages {
  flex: 1;
  overflow: auto;
  padding: 28px;
}
.welcome {
  text-align: center;
  max-width: 620px;
  margin: 50px auto;
}
.welcome h3 {
  margin: 16px 0 7px;
}
.welcome p {
  color: #80909c;
}
.prompts {
  display: flex;
  justify-content: center;
  gap: 9px;
  flex-wrap: wrap;
  margin-top: 25px;
}
.prompts button {
  border: 1px solid #d9e6e3;
  border-radius: 20px;
  background: #fff;
  color: #56706b;
  padding: 9px 14px;
  cursor: pointer;
}
.prompts button:hover {
  border-color: #009688;
  color: #007f73;
}
.message-row {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin: 10px 0;
}
.message-row.user {
  align-items: flex-end;
}
.message-row .message {
  max-width: 70%;
  padding: 11px 14px;
  border-radius: 12px;
  white-space: pre-wrap;
  line-height: 1.65;
}
.message-row.user .message {
  background: #009688;
  color: #fff;
}
.message-row.assistant .message {
  background: #f1f5f4;
  color: #31433f;
}
.message.thinking {
  color: #80908c;
}
.tool-message {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 4px 10px;
  color: #7d8e89;
  font-size: 12px;
}
.draft-card {
  display: grid;
  gap: 7px;
  width: min(440px, 70%);
  margin-top: 8px;
  padding: 14px 16px;
  border: 1px solid #d9e8e5;
  border-radius: 12px;
  background: #fbfefd;
  color: #50625e;
  font-size: 13px;
}
.draft-card b {
  color: #007f73;
  font-size: 14px;
}
.draft-card p {
  margin: 2px 0;
}
.draft-card .el-tag {
  width: fit-content;
}
.composer {
  position: relative;
  padding: 14px 66px 26px 18px;
  border-top: 1px solid #edf0f2;
}
.composer > .el-button {
  position: absolute;
  right: 20px;
  top: 22px;
}
.composer small {
  position: absolute;
  left: 20px;
  bottom: 5px;
  color: #a0abb3;
  font-size: 10px;
}
@media (max-width: 760px) {
  .ai-shell {
    grid-template-columns: 1fr;
  }
  .ai-shell > aside {
    display: none;
  }
}
</style>
