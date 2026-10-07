import { listResource, getResource } from "@/api/resource";
import type { PageQuery } from "@/types";
export type ClinicRow = Record<string, any>;
export const formatDate = (value?: string, withTime = false) =>
  value ? value.replace("T", " ").slice(0, withTime ? 16 : 10) : "未记录";
export const accountName = (value?: string) =>
  ({ ADMIN: "管理员", STAFF: "诊所员工", OWNER: "宠物主人" })[value || ""] ||
  value ||
  "未记录";
export function ageText(date?: string): string {
  if (!date) return "年龄未记录";
  const born = new Date(date),
    now = new Date();
  if (Number.isNaN(born.getTime()) || born > now) return "年龄未记录";
  let years = now.getFullYear() - born.getFullYear();
  if (
    now.getMonth() < born.getMonth() ||
    (now.getMonth() === born.getMonth() && now.getDate() < born.getDate())
  )
    years--;
  if (years > 0) return years + "岁";
  const months = Math.max(
    0,
    (now.getFullYear() - born.getFullYear()) * 12 +
      now.getMonth() -
      born.getMonth() -
      (now.getDate() < born.getDate() ? 1 : 0),
  );
  return months > 0 ? months + "个月" : "不足1个月";
}
export async function listAll<T = ClinicRow>(
  endpoint: string,
  params: PageQuery & Record<string, unknown> = {},
): Promise<T[]> {
  const collected: T[] = [];
  for (let page = 1; ; page++) {
    const response = await listResource<T>(endpoint, {
      ...params,
      page,
      size: 100,
    });
    if (Array.isArray(response.data)) return response.data;
    collected.push(...response.data.records);
    if (
      !response.data.records.length ||
      collected.length >= response.data.total
    )
      return collected;
  }
}
export async function resolveRows(
  endpoint: string,
  ids: number[],
  known: ClinicRow[],
): Promise<ClinicRow[]> {
  const missing = [...new Set(ids)].filter(
    (id) => id && !known.some((row) => row.id === id),
  );
  const results = await Promise.allSettled(
    missing.map((id) => getResource<ClinicRow>(endpoint, id)),
  );
  return [
    ...known,
    ...results.flatMap((result) =>
      result.status === "fulfilled" ? [result.value.data] : [],
    ),
  ];
}
export function flattenTree(rows: ClinicRow[]): ClinicRow[] {
  const result: ClinicRow[] = [],
    seen = new Set<number>();
  const walk = (nodes: ClinicRow[], parentId?: number) =>
    nodes.forEach((node) => {
      if (seen.has(node.id)) return;
      seen.add(node.id);
      const { children, ...row } = node;
      result.push({ ...row, parentId: row.parentId ?? parentId });
      if (Array.isArray(children)) walk(children, node.id);
    });
  walk(rows);
  return result;
}
export function buildTree(rows: ClinicRow[]): ClinicRow[] {
  rows = flattenTree(rows);
  const index = new Map<number, ClinicRow>(
    rows.map((row) => [row.id, { ...row, children: [] as ClinicRow[] }]),
  );
  const roots: ClinicRow[] = [];
  for (const row of index.values()) {
    let ancestor = index.get(row.parentId),
      cycle = row.parentId === row.id;
    const visited = new Set([row.id]);
    while (ancestor && !cycle) {
      if (visited.has(ancestor.id)) {
        cycle = true;
        break;
      }
      visited.add(ancestor.id);
      ancestor = index.get(ancestor.parentId);
    }
    const parent = index.get(row.parentId);
    if (parent && !cycle) parent.children.push(row);
    else roots.push(row);
  }
  return roots;
}
