import { ApiError } from './api';

/** 后端业务错误码 → 用户可读提示（对应 docs/错误码.md 的 QA 段）。 */
const MESSAGES: Record<string, string> = {
  'QA-0101': '问题不存在，可能已被删除',
  'QA-0102': '回答不存在，可能已被删除',
  'QA-0103': '被回复的评论不存在',
  'QA-0201': '问题已删除，无法再提交回答',
  'QA-0203': '你没有权限编辑这条回答',
  'QA-0204': '只有课程教师或助教可以认证回答',
  'QA-0205': '这条回答已经被教师认证过',
  'QA-0206': '已采纳的回答不能由作者本人删除',
  'QA-0207': '你没有权限删除这条回答',
  'QA-0208': '这条回答已被删除，不能继续操作',
  'QA-0301': '不能重复投相同的票',
  'QA-0302': '投票只支持赞成、反对或撤销',
  'QA-0303': '不能给自己的内容投票',
  'QA-0401': '评论内容不能为空',
  'QA-0402': '评论不能超过 500 个字符',
  'QA-0403': '只支持一层评论回复',
  'QA-0404': '只能回复同一内容下的评论',
};

/** 把 ApiError 转成可直接展示给用户的中文提示。 */
export function friendlyError(error: unknown): string {
  if (error instanceof ApiError) {
    return MESSAGES[error.code] ? `${MESSAGES[error.code]}（${error.code}）` : `${error.message}（${error.code}）`;
  }
  return error instanceof Error ? error.message : '操作失败，请稍后重试';
}
