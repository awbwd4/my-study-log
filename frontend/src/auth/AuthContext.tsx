import { createContext, useContext, useState, type ReactNode } from "react";

export type UserType = "TEACHER" | "STUDENT";

interface AuthState {
  token: string | null;
  type: UserType | null;
}

interface AuthContextValue extends AuthState {
  login: (token: string, type: UserType) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({
    token: localStorage.getItem("token"),
    type: (localStorage.getItem("userType") as UserType | null) ?? null,
  });

  const login = (token: string, type: UserType) => {
    localStorage.setItem("token", token);
    localStorage.setItem("userType", type);
    setState({ token, type });
  };

  const logout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("userType");
    setState({ token: null, type: null });
  };

  return <AuthContext.Provider value={{ ...state, login, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
