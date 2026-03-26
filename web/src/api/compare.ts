import { client } from "./client"
import type { TraceComparison } from "../types"

export async function fetchComparison(traceId1: string, traceId2: string): Promise<TraceComparison> {
  const { data } = await client.get<TraceComparison>("/traces/compare", {
    params: { traceId1, traceId2 }
  })
  return data
}
