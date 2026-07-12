export {};

declare global {
  interface Window {
    Kakao?: {
      init: (jsKey: string) => void;
      isInitialized: () => boolean;
      Auth: {
        login: (options: {
          success: (authObj: { access_token: string }) => void;
          fail: (error: unknown) => void;
        }) => void;
      };
    };
  }
}
