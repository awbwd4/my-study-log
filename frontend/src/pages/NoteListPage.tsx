import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { apiClient } from "../api/client";

interface Note {
  id: number;
  studentId: number;
  createdAt: string;
}

export function NoteListPage() {
  const { studentId } = useParams();
  const [notes, setNotes] = useState<Note[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

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

  const createNote = async () => {
    const { data } = await apiClient.post<Note>("/api/notes", {
      studentId: studentId ? Number(studentId) : undefined,
    });
    setNotes((prev) => [...prev, data]);
  };

  if (loading) return <p style={{ padding: 16 }}>불러오는 중...</p>;

  return (
    <div style={{ padding: 16 }}>
      <h2>오답노트</h2>
      {error && <p style={{ color: "red" }}>{error}</p>}
      <button onClick={createNote}>+ 새 오답노트</button>
      <ul>
        {notes.map((note) => (
          <li key={note.id}>
            <Link to={`/notes/${note.id}`}>
              오답노트 #{note.id} · {new Date(note.createdAt).toLocaleString()}
            </Link>
          </li>
        ))}
      </ul>
      {notes.length === 0 && <p>아직 오답노트가 없습니다.</p>}
    </div>
  );
}
