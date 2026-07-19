import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
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

interface Note {
  id: number;
  studentId: number;
  createdAt: string;
  detail: NoteDetail | null;
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

function formFromDetail(detail: NoteDetail | null) {
  return {
    body: detail?.body ?? "",
    submittedAnswer: detail?.submittedAnswer ?? "",
    isActuallyWrong: detail?.isActuallyWrong ?? true,
    questionType: (detail?.questionType ?? "MULTIPLE_CHOICE") as QuestionType,
    registeredDate: detail?.registeredDate ?? new Date().toISOString().slice(0, 10),
    isRetake: detail?.isRetake ?? false,
    originalQuestionKey: detail?.originalQuestionKey ?? "",
  };
}

export function NoteDetailPage() {
  const { noteId } = useParams();
  const navigate = useNavigate();
  const [note, setNote] = useState<Note | null>(null);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState(formFromDetail(null));
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [isRecognizing, setIsRecognizing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [words, setWords] = useState<WordEntry[] | null>(null);
  const [newWord, setNewWord] = useState({ word: "", meaning: "" });

  const load = async () => {
    setLoading(true);
    try {
      const { data } = await apiClient.get<Note>(`/api/notes/${noteId}`);
      setNote(data);
      setForm(formFromDetail(data.detail));
    } catch {
      setError("오답노트를 불러오지 못했습니다");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [noteId]);

  const loadWords = async () => {
    if (!note?.detail) return;
    const { data } = await apiClient.get<WordEntry[]>(`/api/notes/${noteId}/details/${note.detail.id}/words`);
    setWords(data);
  };

  const handleImageSelected = async (file: File | null) => {
    setImageFile(file);
    if (!file) return;

    setIsRecognizing(true);
    try {
      const photoData = new FormData();
      photoData.append("image", file);
      const { data } = await apiClient.post<{ text: string | null }>("/api/ocr", photoData, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      if (data.text) {
        setForm((prev) => (prev.body.trim() ? prev : { ...prev, body: data.text as string }));
      }
    } catch {
      // 즉시 인식 실패는 조용히 무시 — 저장 시 서버에서 한 번 더 시도한다
    } finally {
      setIsRecognizing(false);
    }
  };

  const save = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!note) return;
    setError(null);

    const payload = new FormData();
    payload.append("data", new Blob([JSON.stringify(form)], { type: "application/json" }));
    if (imageFile) payload.append("image", imageFile);

    setIsSaving(true);
    try {
      if (note.detail) {
        await apiClient.put(`/api/notes/${noteId}/details/${note.detail.id}`, payload, {
          headers: { "Content-Type": "multipart/form-data" },
        });
      } else {
        await apiClient.post(`/api/notes/${noteId}/details`, payload, {
          headers: { "Content-Type": "multipart/form-data" },
        });
      }
      setImageFile(null);
      await load();
    } catch {
      setError("저장에 실패했습니다");
    } finally {
      setIsSaving(false);
    }
  };

  const addWord = async () => {
    if (!note?.detail || !newWord.word || !newWord.meaning) return;
    await apiClient.post(`/api/notes/${noteId}/details/${note.detail.id}/words`, {
      items: [{ word: newWord.word, meaning: newWord.meaning }],
    });
    setNewWord({ word: "", meaning: "" });
    loadWords();
  };

  if (loading) return <p style={{ padding: 16 }}>불러오는 중...</p>;
  if (!note) return <p style={{ padding: 16, color: "red" }}>{error ?? "오답노트를 찾을 수 없습니다"}</p>;

  return (
    <div style={{ padding: 16, maxWidth: 480 }}>
      <button onClick={() => navigate(-1)} style={{ marginBottom: 12 }}>
        ← 목록으로
      </button>
      <h2>오답노트 #{noteId}</h2>

      <form onSubmit={save} style={{ border: "1px solid #ddd", padding: 12 }}>
        <textarea
          placeholder="문제 본문 (사진만 첨부해도 괜찮아요)"
          value={form.body}
          onChange={(e) => setForm({ ...form, body: e.target.value })}
          style={{ width: "100%", minHeight: 120, marginBottom: 4 }}
        />
        <p style={{ fontSize: 12, color: "#888", marginTop: 0, marginBottom: 8 }}>
          사진을 새로 첨부하면 문제 텍스트를 자동으로 다시 인식합니다
        </p>
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

        {note.detail?.imagePath && !imageFile && (
          <img
            src={`${API_BASE}${note.detail.imagePath}`}
            alt="문제 사진"
            style={{ maxWidth: 240, display: "block", marginBottom: 8 }}
          />
        )}
        <input
          type="file"
          accept="image/*"
          capture="environment"
          onChange={(e) => handleImageSelected(e.target.files?.[0] ?? null)}
          style={{ width: "100%", marginBottom: 4 }}
        />
        {isRecognizing && <p style={{ fontSize: 12, color: "#888", marginTop: 0, marginBottom: 8 }}>사진에서 텍스트 인식 중...</p>}
        {error && <p style={{ color: "red", fontSize: 13 }}>{error}</p>}
        <button type="submit" disabled={isSaving || isRecognizing}>
          {isSaving ? "저장 중..." : "저장"}
        </button>
      </form>

      {note.detail && (
        <div style={{ marginTop: 16 }}>
          <button onClick={loadWords}>단어장 보기</button>
          {words && (
            <ul>
              {words.flatMap((entry) =>
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
            value={newWord.word}
            onChange={(e) => setNewWord({ ...newWord, word: e.target.value })}
          />
          <input
            placeholder="뜻"
            value={newWord.meaning}
            onChange={(e) => setNewWord({ ...newWord, meaning: e.target.value })}
          />
          <button onClick={addWord}>단어 추가</button>
        </div>
      )}
    </div>
  );
}
