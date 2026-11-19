import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../auth';
import { friendlyError } from '../errors';
import type { QuestionDetail } from '../types';

export function QuestionDetailPage() {
  const { id } = useParams();
  const questionId = Number(id);
  const { me } = useAuth();
  const [q, setQ] = useState<QuestionDetail | null>(null);
  const [related, setRelated] = useState<{ id: number; title: string }[]>([]);
  const [draft, setDraft] = useState('');
  const [error, setError] = useState('');
  // 回答编辑态：正在编辑的回答 id 与草稿
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editDraft, setEditDraft] = useState('');
  // 问题编辑态
  const [editingQuestion, setEditingQuestion] = useState(false);
  const [questionDraft, setQuestionDraft] = useState({ title: '', body: '', tags: '' });

  const load = useCallback(() => {
    api.question(questionId).then(setQ).catch((e) => setError(friendlyError(e)));
  }, [questionId]);

  useEffect(() => {
    load();
    api.related(questionId).then(setRelated).catch(() => undefined);
  }, [load, questionId]);

  const act = (fn: () => Promise<unknown>) => fn().then(load).catch((e) => setError(friendlyError(e)));

  if (!q) return <p className="muted">{error || '加载中…'}</p>;

  const canManage = q.permissions.canManage;

  return (
    <div className="layout">
      <article>
        {/* 问题编辑：仅作者或课程管理者可见（后端仍会在领域层再校验一次） */}
        {editingQuestion ? (
          <section className="answer-form">
            <h3>编辑问题</h3>
            <input
              value={questionDraft.title}
              onChange={(e) => setQuestionDraft({ ...questionDraft, title: e.target.value })}
              placeholder="标题（10–150 字）"
            />
            <textarea
              rows={8}
              value={questionDraft.body}
              onChange={(e) => setQuestionDraft({ ...questionDraft, body: e.target.value })}
            />
            <input
              value={questionDraft.tags}
              onChange={(e) => setQuestionDraft({ ...questionDraft, tags: e.target.value })}
              placeholder="标签，用英文逗号分隔，1–5 个"
            />
            <div className="actions">
              <button className="primary" onClick={() => act(async () => {
                await api.editQuestion(q.id, {
                  courseId: q.course?.id,
                  title: questionDraft.title,
                  body: questionDraft.body,
                  tags: questionDraft.tags.split(',').map((t) => t.trim()).filter(Boolean),
                });
                setEditingQuestion(false);
              })}>保存修改
              </button>
              <button className="link" onClick={() => setEditingQuestion(false)}>取消</button>
            </div>
          </section>
        ) : (
          <>
            <h1>{q.title}</h1>
            <p className="muted">
              {q.author.displayName} · {new Date(q.createdAt).toLocaleString('zh-CN')} · {q.viewCount} 次浏览
              {q.course && <> · 课程 {q.course.name}</>}
            </p>
            {(q.permissions.canEdit || q.permissions.canDelete) && (
              <div className="actions">
                {q.permissions.canEdit && (
                  <button className="link" onClick={() => {
                    setQuestionDraft({ title: q.title, body: q.body, tags: q.tags.join(', ') });
                    setEditingQuestion(true);
                  }}>编辑问题
                  </button>
                )}
                {q.permissions.canDelete && (
                  <button className="link" onClick={() => {
                    if (confirm('确定删除这个问题？删除后不可恢复。')) act(() => api.deleteQuestion(q.id));
                  }}>删除问题
                  </button>
                )}
              </div>
            )}
          </>
        )}

        <div className="post">
          <div className="votebox">
            <button disabled={!me} onClick={() => act(() => api.vote('QUESTION', q.id, q.myVote === 1 ? 0 : 1))}>▲</button>
            <b>{q.score}</b>
            <button disabled={!me} onClick={() => act(() => api.vote('QUESTION', q.id, q.myVote === -1 ? 0 : -1))}>▼</button>
          </div>
          <div className="body">
            {/* 正文由后端渲染并经 commonmark + OWASP Sanitizer 双重净化 */}
            <div dangerouslySetInnerHTML={{ __html: q.bodyHtml }} />
            <div className="tagrow">{q.tags.map((t) => <span key={t} className="tag">{t}</span>)}</div>
            <CommentList comments={q.comments} targetType="QUESTION" targetId={q.id} onDone={load} canComment={!!me} />
            {canManage && (
              <button className="link" onClick={() => {
                const points = Number(prompt('悬赏积分（10–500）', '50'));
                if (points) act(() => api.bounty(q.id, points, 7));
              }}>发起悬赏
              </button>
            )}
          </div>
        </div>

        <h2>{q.answerCount} 个回答</h2>
        {q.answers.map((a) => (
          <div className={`post ${q.acceptedAnswerId === a.id ? 'is-accepted' : ''}`} key={a.id} id={`answer-${a.id}`}>
            <div className="votebox">
              <button disabled={!me} onClick={() => act(() => api.vote('ANSWER', a.id, a.myVote === 1 ? 0 : 1))}>▲</button>
              <b>{a.score}</b>
              <button disabled={!me} onClick={() => act(() => api.vote('ANSWER', a.id, a.myVote === -1 ? 0 : -1))}>▼</button>
              {q.acceptedAnswerId === a.id && <span className="check-mark" title="已采纳">✔</span>}
            </div>
            <div className="body">
              {editingId === a.id ? (
                <div className="answer-form">
                  <textarea rows={6} value={editDraft} onChange={(e) => setEditDraft(e.target.value)} />
                  <div className="actions">
                    <button className="primary" onClick={() => act(async () => {
                      await api.editAnswer(a.id, editDraft);
                      setEditingId(null);
                    })}>保存修改
                    </button>
                    <button className="link" onClick={() => setEditingId(null)}>取消</button>
                  </div>
                </div>
              ) : (
                <div dangerouslySetInnerHTML={{ __html: a.bodyHtml }} />
              )}
              <p className="muted">
                {a.author.displayName}
                {a.endorsedBy && <span className="endorsed">教师认证 · {a.endorsedBy.displayName}</span>}
                · {new Date(a.createdAt).toLocaleString('zh-CN')}
              </p>
              <div className="actions">
                {q.permissions.canAccept && !q.acceptedAnswerId &&
                  <button onClick={() => act(() => api.accept(q.id, a.id))}>采纳此回答</button>}
                {canManage && !a.endorsedBy && <button onClick={() => act(() => api.endorse(a.id))}>教师认证</button>}
                {/* a.canEdit 由后端按"作者本人或课程管理者"计算 */}
                {a.canEdit && editingId !== a.id && (
                  <button className="link" onClick={() => { setEditingId(a.id); setEditDraft(a.body); }}>编辑回答</button>
                )}
                {a.canEdit && (
                  <button className="link" onClick={() => {
                    if (confirm('确定删除这条回答？删除后不可恢复。')) act(() => api.deleteAnswer(a.id));
                  }}>删除回答
                  </button>
                )}
              </div>
              <CommentList comments={a.comments} targetType="ANSWER" targetId={a.id} onDone={load} canComment={!!me} />
            </div>
          </div>
        ))}

        {me ? (
          <section className="answer-form">
            <h3>写回答</h3>
            <textarea rows={8} value={draft} onChange={(e) => setDraft(e.target.value)}
                      placeholder="支持 Markdown，代码请使用 ``` 包裹" />
            <button className="primary" onClick={() => act(async () => {
              await api.answer(q.id, draft);
              setDraft('');
            })}>提交回答
            </button>
          </section>
        ) : <p className="muted"><Link to="/login">登录</Link>后可以回答与投票。</p>}
        {error && <p className="error">{error}</p>}
      </article>

      <aside>
        <h3>相关问题</h3>
        <ul className="plain">
          {related.map((r) => <li key={r.id}><Link to={`/questions/${r.id}`}>{r.title}</Link></li>)}
          {related.length === 0 && <li className="muted">暂无</li>}
        </ul>
      </aside>
    </div>
  );
}

function CommentList({ comments, targetType, targetId, onDone, canComment }: {
  comments: { id: number; author: { displayName: string }; parentId?: number; body: string; createdAt: string }[];
  targetType: 'QUESTION' | 'ANSWER';
  targetId: number;
  onDone: () => void;
  canComment: boolean;
}) {
  const [text, setText] = useState('');
  const [open, setOpen] = useState(false);
  const [replyTo, setReplyTo] = useState<number | null>(null);
  const [error, setError] = useState('');

  const submit = () => api.comment(targetType, targetId, text, replyTo ?? undefined)
    .then(() => { setText(''); setReplyTo(null); setOpen(false); setError(''); onDone(); })
    .catch((e) => setError(friendlyError(e)));

  return (
    <div className="comments">
      {comments.map((c) => (
        <div className="comment" key={c.id} style={c.parentId ? { marginLeft: '1.5rem' } : undefined}>
          <span>{c.body}</span>
          <span className="muted"> — {c.author.displayName}</span>
          {/* 只允许对一级评论回复：二级回复不能再挂子回复（后端 QA-0403 兜底） */}
          {canComment && !c.parentId && (
            <button className="link" onClick={() => { setReplyTo(c.id); setOpen(true); }}>
              回复
            </button>
          )}
        </div>
      ))}
      {canComment && (open ? (
        <div className="comment-form">
          {replyTo !== null && <span className="muted">回复 #{replyTo}：</span>}
          <input value={text} onChange={(e) => setText(e.target.value)} placeholder="评论，可用 @昵称 提醒他人" />
          <button onClick={submit}>发表</button>
          <button className="link" onClick={() => { setOpen(false); setReplyTo(null); setText(''); setError(''); }}>
            取消
          </button>
        </div>
      ) : <button className="link" onClick={() => setOpen(true)}>添加评论</button>)}
      {error && <p className="error">{error}</p>}
    </div>
  );
}
