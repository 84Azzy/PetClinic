<template>
  <div class="ai-page">
    <PageHeader
      title="AI 诊疗助手"
      description="预留 AI 模块：自然语言查询与结构化预约草稿"
    />
    <div class="ai-shell">
      <aside>
        <div class="new-chat">
          <el-button type="primary" :icon="Plus" @click="newChat"
            >新对话</el-button
          >
        </div>
        <div
          v-for="c in conversations"
          :key="c.id"
          :class="['conversation', { active: c.id === conversationId }]"
          @click="conversationId = c.id"
        >
          <el-icon><ChatDotRound /></el-icon><span>{{ c.title }}</span>
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
          <el-tag type="warning" effect="plain">待你实现</el-tag>
        </div>
        <div class="messages">
          <div class="welcome">
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
          <div v-for="(m, i) in messages" :key="i" :class="['message', m.role]">
            {{ m.content }}
          </div>
        </div>
        <div class="composer">
          <el-input
            v-model="input"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 4 }"
            placeholder="例如：给我的猫找明天下午的外科医生"
            @keydown.ctrl.enter="send"
          /><el-button
            type="primary"
            circle
            :icon="Promotion"
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
import { ref } from "vue";
import { Plus, Promotion } from "@element-plus/icons-vue";
import PageHeader from "@/components/PageHeader.vue";
import http from "@/api/http";
const conversationId = ref<number>();
const conversations = ref([
  { id: 1, title: "糯米的就诊安排" },
  { id: 2, title: "查询外科医生" },
]);
const messages = ref<{ role: string; content: string }[]>([]);
const input = ref("");
const prompts = ["查询我的宠物", "查找明天下午可用兽医", "查看我的预约"];
const newChat = () => {
  conversationId.value = undefined;
  messages.value = [];
};
const send = async () => {
  if (!input.value.trim()) return;
  const content = input.value;
  messages.value.push({ role: "user", content });
  input.value = "";
  await http.post("/ai/chat", {
    conversationId: conversationId.value,
    message: content,
  });
};
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
.message {
  max-width: 70%;
  padding: 11px 14px;
  border-radius: 12px;
  margin: 10px;
}
.message.user {
  margin-left: auto;
  background: #009688;
  color: #fff;
}
.message.assistant {
  background: #f1f5f4;
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
