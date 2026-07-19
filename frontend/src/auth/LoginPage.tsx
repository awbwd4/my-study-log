import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { apiClient } from "../api/client";
import { useAuth, type UserType } from "./AuthContext";

const KAKAO_JS_KEY = import.meta.env.VITE_KAKAO_JS_KEY as string | undefined;

interface AuthResult {
  status: "OK" | "NEEDS_PROFILE";
  token: string;
  type: UserType | null;
}

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [kakaoReady, setKakaoReady] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [devKakaoId, setDevKakaoId] = useState("");
  const [devType, setDevType] = useState<UserType>("STUDENT");
  const [devName, setDevName] = useState("");
  const [devAcademyName, setDevAcademyName] = useState("");
  const [devPhone, setDevPhone] = useState("");

  useEffect(() => {
    if (!KAKAO_JS_KEY || !window.Kakao) return;
    if (!window.Kakao.isInitialized()) {
      window.Kakao.init(KAKAO_JS_KEY);
    }
    setKakaoReady(true);
  }, []);

  const handleAuthResult = (result: AuthResult) => {
    if (result.status === "NEEDS_PROFILE") {
      sessionStorage.setItem("tempToken", result.token);
      navigate("/complete-profile");
      return;
    }
    login(result.token, result.type as UserType);
    navigate("/");
  };

  const handleKakaoLogin = () => {
    setError(null);
    window.Kakao?.Auth.login({
      success: async (authObj) => {
        try {
          const { data } = await apiClient.post<AuthResult>("/api/auth/kakao", {
            accessToken: authObj.access_token,
          });
          handleAuthResult(data);
        } catch (err) {
          console.error("Kakao login backend exchange failed:", err);
          setError("카카오 로그인 처리 중 오류가 발생했습니다");
        }
      },
      fail: (err) => {
        console.error("Kakao.Auth.login failed:", err);
        setError(`카카오 로그인에 실패했습니다: ${JSON.stringify(err)}`);
      },
    });
  };

  const handleDevLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      const { data } = await apiClient.post<AuthResult>("/api/auth/dev-login", {
        kakaoId: devKakaoId,
        type: devType,
        name: devName,
        academyName: devType === "TEACHER" ? devAcademyName : undefined,
        phone: devType === "STUDENT" ? devPhone : undefined,
      });
      handleAuthResult(data);
    } catch {
      setError("개발자 로그인 중 오류가 발생했습니다");
    }
  };

  return (
    <div style={{ maxWidth: 360, margin: "80px auto", padding: 16 }}>
      <h1>my-study-log</h1>

      <button onClick={handleKakaoLogin} disabled={!kakaoReady} style={{ width: "100%", padding: 12 }}>
        카카오로 시작하기
      </button>
      {!KAKAO_JS_KEY && (
        <p style={{ fontSize: 12, color: "#888" }}>
          VITE_KAKAO_JS_KEY가 설정되지 않아 카카오 로그인은 비활성화되어 있습니다. 아래 개발자 로그인을 사용하세요.
        </p>
      )}

      {error && <p style={{ color: "red" }}>{error}</p>}

      {(import.meta.env.DEV || import.meta.env.VITE_ENABLE_DEV_LOGIN === "true") && (
        <form onSubmit={handleDevLogin} style={{ marginTop: 32, borderTop: "1px solid #ccc", paddingTop: 16 }}>
          <h3>개발자 로그인 (dev 전용)</h3>
          <input
            placeholder="kakaoId (임의 문자열)"
            value={devKakaoId}
            onChange={(e) => setDevKakaoId(e.target.value)}
            required
            style={{ width: "100%", marginBottom: 8 }}
          />
          <select value={devType} onChange={(e) => setDevType(e.target.value as UserType)} style={{ width: "100%", marginBottom: 8 }}>
            <option value="STUDENT">학생</option>
            <option value="TEACHER">강사</option>
          </select>
          <input
            placeholder="이름"
            value={devName}
            onChange={(e) => setDevName(e.target.value)}
            required
            style={{ width: "100%", marginBottom: 8 }}
          />
          {devType === "TEACHER" ? (
            <input
              placeholder="학원명"
              value={devAcademyName}
              onChange={(e) => setDevAcademyName(e.target.value)}
              required
              style={{ width: "100%", marginBottom: 8 }}
            />
          ) : (
            <input
              placeholder="전화번호"
              value={devPhone}
              onChange={(e) => setDevPhone(e.target.value)}
              required
              style={{ width: "100%", marginBottom: 8 }}
            />
          )}
          <button type="submit" style={{ width: "100%", padding: 8 }}>
            개발자 로그인
          </button>
        </form>
      )}
    </div>
  );
}
