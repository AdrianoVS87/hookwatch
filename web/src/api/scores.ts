import { client } from './client'

export interface AutoEvalResponse {
  evaluatedNow: number
  skippedAlreadyEvaluated: number
  averageScore: number
}

export async function autoEvaluateAgent(agentId: string, limit = 50): Promise<AutoEvalResponse> {
  const { data } = await client.post(`/agents/${agentId}/scores/auto-evaluate`, null, {
    params: { limit },
  })
  return data
}
