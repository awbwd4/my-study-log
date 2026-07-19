import { useEffect, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
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

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8081";

export function NoteListPage() {
  const { studentId } = useParams();
  const [notes, setNotes] = useState<Note[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState<{ total: number } | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const load = async () => {
    setLoading(true);
    try {
      const { data } = await apiClient.get<Note[]>("/api/notes", {
        params: studentId ? { studentId } : undefined,
      });
      setNotes(data);
    } catch {
      setError("오답노트 목록을 불러오지 못했습니다");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [studentId]);

  const handleFilesSelected = async (files: FileList | null) => {
    if (!files || files.length === 0) return;
    setError(null);
    setUploadProgress({ total: files.length });
    setIsUploading(true);

    const payload = new FormData();
    Array.from(files).forEach((file) => payload.append("images", file));
    if (studentId) payload.append("studentId", studentId);

    try {
      await apiClient.post("/api/notes/batch", payload, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      await load();
    } catch {
      setError("오답노트 등록에 실패했습니다");
    } finally {
      setIsUploading(false);
      setUploadProgress(null);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  };

  if (loading) return <p style={{ padding: 16 }}>불러오는 중...</p>;

  return (
    <div style={{ padding: 16 }}>
      <h2>오답노트</h2>
      {error && <p style={{ color: "red" }}>{error}</p>}

      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        multiple
        style={{ display: "none" }}
        onChange={(e) => handleFilesSelected(e.target.files)}
      />
      <button onClick={() => fileInputRef.current?.click()} disabled={isUploading} style={{ padding: 10 }}>
        {isUploading
          ? `오답노트 등록 중... (사진 ${uploadProgress?.total ?? 0}장 인식 중, 시간이 좀 걸려요)`
          : "+ 오답노트 등록 (사진 여러 장 선택 가능)"}
      </button>

      <div style={{ marginTop: 16 }}>
        {notes.map((note) => (
          <Link
            key={note.id}
            to={`/notes/${note.id}`}
            style={{
              display: "flex",
              gap: 12,
              border: "1px solid #eee",
              padding: 12,
              marginBottom: 8,
              textDecoration: "none",
              color: "inherit",
            }}
          >
            {note.detail?.imagePath && (
              <img
                src={`${API_BASE}${note.detail.imagePath}`}
                alt="문제 사진"
                style={{ width: 64, height: 64, objectFit: "cover", flexShrink: 0 }}
              />
            )}
            <div style={{ minWidth: 0 }}>
              <p
                style={{
                  margin: 0,
                  whiteSpace: "nowrap",
                  overflow: "hidden",
                  textOverflow: "ellipsis",
                  fontWeight: "bold",
                }}
              >
                {note.detail?.body?.trim() || "(본문 없음 — 눌러서 채워주세요)"}
              </p>
              <p style={{ margin: 0, fontSize: 12, color: "#888" }}>
                {new Date(note.createdAt).toLocaleString()}
                {note.detail && !note.detail.submittedAnswer && " · 제출답안 미입력"}
              </p>
            </div>
          </Link>
        ))}
      </div>
      {notes.length === 0 && <p>아직 오답노트가 없습니다. 위 버튼으로 문제 사진을 올려보세요.</p>}
    </div>
  );
}
