import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { apiClient } from "../api/client";

interface SchoolClass {
  id: number;
  dayOfWeek: string;
  time: string;
  grade: string;
}

interface Student {
  id: number;
  name: string;
  phone: string;
}

export function TeacherClassesPage() {
  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [form, setForm] = useState({ dayOfWeek: "MON", time: "18:00:00", grade: "" });
  const [studentsByClass, setStudentsByClass] = useState<Record<number, Student[]>>({});

  const loadClasses = async () => {
    const { data } = await apiClient.get<SchoolClass[]>("/api/classes");
    setClasses(data);
  };

  useEffect(() => {
    loadClasses();
  }, []);

  const createClass = async (e: React.FormEvent) => {
    e.preventDefault();
    await apiClient.post("/api/classes", form);
    setForm({ dayOfWeek: "MON", time: "18:00:00", grade: "" });
    loadClasses();
  };

  const loadStudents = async (classId: number) => {
    const { data } = await apiClient.get<Student[]>("/api/students", { params: { classId } });
    setStudentsByClass((prev) => ({ ...prev, [classId]: data }));
  };

  return (
    <div style={{ padding: 16 }}>
      <h2>반 관리</h2>

      <form onSubmit={createClass} style={{ border: "1px solid #ddd", padding: 12, marginBottom: 24 }}>
        <h3>반 추가</h3>
        <select value={form.dayOfWeek} onChange={(e) => setForm({ ...form, dayOfWeek: e.target.value })} style={{ marginRight: 8 }}>
          {["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"].map((d) => (
            <option key={d} value={d}>
              {d}
            </option>
          ))}
        </select>
        <input
          type="time"
          value={form.time.slice(0, 5)}
          onChange={(e) => setForm({ ...form, time: `${e.target.value}:00` })}
          style={{ marginRight: 8 }}
        />
        <input
          placeholder="학년"
          value={form.grade}
          onChange={(e) => setForm({ ...form, grade: e.target.value })}
          required
          style={{ marginRight: 8 }}
        />
        <button type="submit">추가</button>
      </form>

      {classes.map((c) => (
        <div key={c.id} style={{ border: "1px solid #eee", padding: 12, marginBottom: 12 }}>
          <p>
            <strong>
              {c.dayOfWeek} {c.time.slice(0, 5)} · {c.grade}
            </strong>
          </p>
          <button onClick={() => loadStudents(c.id)}>학생 목록 보기</button>
          <ul>
            {studentsByClass[c.id]?.map((s) => (
              <li key={s.id}>
                {s.name} ({s.phone}) — <Link to={`/students/${s.id}/notes`}>오답노트 보기</Link>
              </li>
            ))}
          </ul>
        </div>
      ))}
    </div>
  );
}
