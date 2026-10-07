<template>
  <svg
    width="24"
    height="24"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.7"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <path v-for="(path, index) in paths" :key="index" :d="path" />
  </svg>
</template>
<script setup lang="ts">
import { computed } from "vue";
const props = defineProps<{ name?: string }>();
const icons: Record<string, string[]> = {
  arrow: ["M4 12h16m-6-6 6 6-6 6"],
  "chevron-left": ["m15 5-7 7 7 7"],
  "chevron-right": ["m9 5 7 7-7 7"],
  home: ["M3 10 12 3l9 7", "M5 9v12h5v-7h4v7h5V9"],
  pet: [
    "M7 13c-1 2-3 3-3 5s2 3 4 2l4-1 4 1c2 1 4 0 4-2s-2-3-3-5c-2-3-8-3-10 0Z",
    "M8 4c2 0 2 6 0 6S6 4 8 4ZM16 4c2 0 2 6 0 6s-2-6 0-6ZM3 8c2-1 4 4 2 5S1 9 3 8ZM21 8c-2-1-4 4-2 5s4-4 2-5Z",
  ],
  person: [
    "M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z",
    "M4 21v-3a6 6 0 0 1 6-6h4a6 6 0 0 1 6 6v3Z",
  ],
  calendar: [
    "M5 5h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2Z",
    "M7 3v4m10-4v4M3 10h18M8 14h2m4 0h2m-8 4h2",
  ],
  doctor: [
    "M15 6a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z",
    "M5 21v-5a7 7 0 0 1 14 0v5ZM9 12v4a3 3 0 0 0 6 0v-4M12 19v2",
  ],
  file: ["M6 3h8l4 4v14H6Z", "M14 3v5h4M9 12h6m-6 4h6"],
  syringe: [
    "m5 19 3-3m-5 5 2-2",
    "m8 16-2-2L16 4l4 4L10 18Zm6-12 4-2m0 0 4 4m-2 4 2-4M11 9l4 4",
  ],
  chat: [
    "M5 4h14a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2h-8l-6 3v-3H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z",
    "M7 8h10M7 12h7",
  ],
  spark: ["m12 3 2.5 6.5L21 12l-6.5 2.5L12 21l-2.5-6.5L3 12l6.5-2.5Z"],
  settings: [
    "M9 3h6l1 3 3 1 2 5-2 5-3 1-1 3H9l-1-3-3-1-2-5 2-5 3-1Z",
    "M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z",
  ],
  list: ["M5 3h14v18H5Z", "M8 7h8M8 11h8M8 15h8M8 18h4"],
  shield: ["M12 3 3 6v6c0 5 9 9 9 9s9-4 9-9V6Z", "m8 12 3 3 5-6"],
  tag: ["M3 3h8l10 10-8 8L3 11ZM7 7h.01"],
};
const byRoute: Record<string, string> = {
  dashboard: "home",
  pets: "pet",
  owners: "person",
  visits: "calendar",
  schedules: "calendar",
  vets: "doctor",
  specialties: "tag",
  "medical-records": "file",
  vaccinations: "syringe",
  notices: "file",
  feedback: "chat",
  ai: "spark",
  "pet-types": "tag",
  "operation-logs": "list",
  profile: "person",
};
const paths = computed(
  () => icons[byRoute[props.name || ""] || props.name || ""] || icons.settings,
);
</script>
