import { afterEach, describe, expect, it, vi } from 'vitest'
import { agentChatApi } from '@/api/modules/agent-chat'

afterEach(() => vi.unstubAllGlobals())

describe('FinWork event stream', () => {
  it('decodes UTF-8 split across network chunks and requires a final event', async () => {
    const bytes = new TextEncoder().encode(
      'event: session\ndata: s-1\n\n' +
      'event: status\ndata: 正在生成回答\n\n' +
      'event: delta\ndata: 你\n\n' +
      'event: delta\ndata: 好\n\n' +
      'event: final\ndata: {"sessionId":"s-1","content":"你好","kind":"CHAT"}\n\n' +
      'event: done\ndata: {}\n\n')
    const split = bytes.indexOf(0xe4) + 1
    const body = new ReadableStream<Uint8Array>({
      start(controller) {
        controller.enqueue(bytes.slice(0, split))
        controller.enqueue(bytes.slice(split))
        controller.close()
      }
    })
    const fetchMock = vi.fn().mockResolvedValue(new Response(body, { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)
    const events: string[] = []
    await agentChatApi.streamMessage('s-1', { message: '你好' }, (event, data) => {
      events.push(`${event}:${typeof data === 'string' ? data : data.content}`)
    })
    expect(events).toEqual(['session:s-1', 'status:正在生成回答', 'delta:你', 'delta:好', 'final:你好', 'done:{}'])
    expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining('/messages/stream'),
      expect.objectContaining({ method: 'POST' }))
  })

  it('rejects a connection that ends before the final event', async () => {
    const body = new ReadableStream<Uint8Array>({
      start(controller) {
        controller.enqueue(new TextEncoder().encode('event: delta\ndata: 部分文字\n\n'))
        controller.close()
      }
    })
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(body, { status: 200 })))
    await expect(agentChatApi.streamMessage('s-1', { message: '你好' }, () => {}))
      .rejects.toThrow('连接中断')
  })
})
