import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { apiClient } from "../api/client";

type QuestionType = "ESSAY" | "MULTIPLE_CHOICE";

interface NoteDetail {
  id: number;
  noteId: number;
  body: string;
  submittedAnswer: string | null;
  isActuallyWrong: boolean;
  questionType: QuestionType;
  registeredDate: string;
  isRetake: boolean;
  originalQuestionKey: string | null;
  imagePath: string | null;
}

interface WordItem {
  word: string;
  meaning: string;
}

interface WordEntry {
  id: number;
  detailId: number;
  items: WordItem[];
}

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8081";

function emptyForm() {
  return {
    body: "",
    submittedAnswer: "",
    isActuallyWrong: true,
    questionType: "MULTIPLE_CHOICE" as QuestionType,
    registeredDate: new Date().toISOString().slice(0, 10),
    isRetake: false,
    originalQuestionKey: "",
  };
}

export function NoteDetailPage() {
  const { noteId } = useParams();
  const [details, setDetails] = useState<NoteDetail[]>([]);
  const [form, setForm] = useState(emptyForm());
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [wordsByDetail, setWordsByDetail] = useState<Record<number, WordEntry[]>>({});
  const [newWord, setNewWord] = useState<Record<number, { word: string; meaning: string }>>({});

  const loadDetails = async () => {
    const { data } = await apiClient.get<NoteDetail[]>(`/api/notes/${noteId}/details`);
    setDetails(data);
  };

  useEffect(() => {
    loadDetails();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [noteId]);

  const loadWords = async (detailId: number) => {
    const { data } = await apiClient.get<WordEntry[]>(`/api/notes/${noteId}/details/${detailId}/words`);
    setWordsByDetail((prev) => ({ ...prev, [detailId]: data }));
  };

  const submitDetail = async (e: React.FormEvent) => {
    e.preventDefault();
    const payload = new FormData();
    payload.append("data", new Blob([JSON.stringify(form)], { type: "application/json" }));
    if (imageFile) payload.append("image", imageFile);

    await apiClient.post(`/api/notes/${noteId}/details`, payload, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    setForm(emptyForm());
    setImageFile(null);
    loadDetails();
  };

  const addWord = async (detailId: number) => {
    const entry = newWord[detailId];
    if (!entry?.word || !entry?.meaning) return;
    await apiClient.post(`/api/notes/${noteId}/details/${detailId}/words`, {
      items: [{ word: entry.word, meaning: entry.meaning }],
    });
    setNewWord((prev) => ({ ...prev, [detailId]: { word: "", meaning: "" } }));
    loadWords(detailId);
  };

  return (
    <div style={{ padding: 16 }}>
      <h2>오답노트 #{noteId}</h2>

      <form onSubmit={submitDetail} style={{ border: "1px solid #ddd", padding: 12, marginBottom: 24 }}>
        <h3>문제 추가</h3>
        <textarea
          placeholder="문제 본문"
          value={form.body}
          onChange={(e) => setForm({ ...form, body: e.target.value })}
          required
          style={{ width: "100%", marginBottom: 8 }}
        />
        <input
          placeholder="제출한 답안"
          value={form.submittedAnswer}
          onChange={(e) => setForm({ ...form, submittedAnswer: e.target.value })}
          style={{ width: "100%", marginBottom: 8 }}
        />
        <label style={{ display: "block", marginBottom: 8 }}>
          <input
            type="checkbox"
            checked={form.isActuallyWrong}
            onChange={(e) => setForm({ ...form, isActuallyWrong: e.target.checked })}
          />{" "}
          실제 오답임
        </label>
        <select
          value={form.questionType}
          onChange={(e) => setForm({ ...form, questionType: e.target.value as QuestionType })}
          style={{ width: "100%", marginBottom: 8 }}
        >
          <option value="MULTIPLE_CHOICE">객관식</option>
          <option value="ESSAY">서술형</option>
        </select>
        <input
          type="date"
          value={form.registeredDate}
          onChange={(e) => setForm({ ...form, registeredDate: e.target.value })}
          style={{ width: "100%", marginBottom: 8 }}
        />
        <label style={{ display: "block", marginBottom: 8 }}>
          <input type="checkbox" checked={form.isRetake} onChange={(e) => setForm({ ...form, isRetake: e.target.checked })} /> 재응시
        </label>
        <input
          placeholder="원문제 key (선택)"
          value={form.originalQuestionKey}
          onChange={(e) => setForm({ ...form, originalQuestionKey: e.target.value })}
          style={{ width: "100%", marginBottom: 8 }}
        />
        <input
          type="file"
          accept="image/*"
          capture="environment"
          onChange={(e) => setImageFile(e.target.files?.[0] ?? null)}
          style={{ width: "100%", marginBottom: 8 }}
        />
        <button type="submit">추가</button>
      </form>

      {details.map((detail) => (
        <div key={detail.id} style={{ border: "1px solid #eee", padding: 12, marginBottom: 12 }}>
          <p>
            <strong>{detail.body}</strong> {detail.isRetake && "(재응시)"}
          </p>
          <p style={{ fontSize: 13, color: "#666" }}>
            제출답안: {detail.submittedAnswer ?? "-"} · {detail.questionType === "ESSAY" ? "서술형" : "객관식"} ·{" "}
            {detail.registeredDate}
          </p>
          {detail.imagePath && (
            <img src={`${API_BASE}${detail.imagePath}`} alt="문제 사진" style={{ maxWidth: 240, display: "block" }} />
          )}

          <div style={{ marginTop: 8 }}>
            <button onClick={() => loadWords(detail.id)}>단어장 보기</button>
            {wordsByDetail[detail.id] && (
              <ul>
                {wordsByDetail[detail.id].flatMap((entry) =>
                  entry.items.map((item, idx) => (
                    <li key={`${entry.id}-${idx}`}>
                      {item.word} - {item.meaning}
                    </li>
                  )),
                )}
              </ul>
            )}
            <input
              placeholder="단어"
              value={newWord[detail.id]?.word ?? ""}
              onChange={(e) =>
                setNewWord((prev) => ({ ...prev, [detail.id]: { word: e.target.value, meaning: prev[detail.id]?.meaning ?? "" } }))
              }
            />
            <input
              placeholder="뜻"
              value={newWord[detail.id]?.meaning ?? ""}
              onChange={(e) =>
                setNewWord((prev) => ({ ...prev, [detail.id]: { word: prev[detail.id]?.word ?? "", meaning: e.target.value } }))
              }
            />
            <button onClick={() => addWord(detail.id)}>단어 추가</button>
          </div>
        </div>
      ))}
    </div>
  );
}
