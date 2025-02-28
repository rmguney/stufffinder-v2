import { defineConfig, loadEnv } from 'vite';
import { sveltekit } from '@sveltejs/kit/vite';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');

  return {
    plugins: [sveltekit()],
    define: {
      VITE_SUPABASE_URL: JSON.stringify(env.VITE_SUPABASE_URL),
      VITE_SUPABASE_ANON_KEY: JSON.stringify(env.VITE_SUPABASE_ANON_KEY)
    },
    server: {
      allowedHosts: ['frontend-310608491068.europe-west1.run.app']
    }
  };
});
