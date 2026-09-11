import http from "./http";
import type { ApiResponse } from "@/types";

export interface AiConversation {
  id: number;
  title: string;
  status: "ACTIVE" | "DELETED";
  createdAt: string;
  updatedAt: string;
}

export interface AppointmentDraft {
  petId: number;
  slotId: number;
  reason: string;
  summary?: string;
}

export interface AiMessage {
  id: number;
  conversationId: number;
  role: "USER" | "ASSISTANT" | "TOOL";
  content: string;
  toolName?: string;
  toolPayload?: string;
  createdAt: string;
}

export interface ChatResponse {
  conversationId: number;
  answer: string;
  draft?: AppointmentDraft;
  toolsUsed: string[];
}

export const listAiConversations = () =>
  http.get<any, ApiResponse<AiConversation[]>>("/ai/conversations");

export const listAiMessages = (conversationId: number) =>
  http.get<any, ApiResponse<AiMessage[]>>(
    `/ai/conversations/${conversationId}/messages`,
  );

export const sendAiMessage = (data: {
  conversationId?: number;
  message: string;
}) =>
  http.post<any, ApiResponse<ChatResponse>>("/ai/chat", data, {
    timeout: 60_000,
  });

export const deleteAiConversation = (conversationId: number) =>
  http.delete<any, ApiResponse<void>>(`/ai/conversations/${conversationId}`);
