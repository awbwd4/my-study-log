import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./auth/AuthContext";
import { CompleteProfilePage } from "./auth/CompleteProfilePage";
import { LoginPage } from "./auth/LoginPage";
import { Nav } from "./components/Nav";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { NoteDetailPage } from "./pages/NoteDetailPage";
import { NoteListPage } from "./pages/NoteListPage";
import { TeacherClassesPage } from "./pages/TeacherClassesPage";

function Home() {
  const { type } = useAuth();
  if (type === "TEACHER") return <Navigate to="/classes" replace />;
  return <NoteListPage />;
}

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/complete-profile" element={<CompleteProfilePage />} />
      <Route
        path="/*"
        element={
          <ProtectedRoute>
            <Nav />
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/classes" element={<TeacherClassesPage />} />
              <Route path="/students/:studentId/notes" element={<NoteListPage />} />
              <Route path="/notes/:noteId" element={<NoteDetailPage />} />
            </Routes>
          </ProtectedRoute>
        }
      />
    </Routes>
  );
}

export default App;
