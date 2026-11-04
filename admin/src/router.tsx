import { createRootRoute, createRoute, createRouter } from '@tanstack/react-router';
import { AppLayout } from './components/layout/AppLayout';
import { Dashboard } from './routes/Dashboard';
import { Users } from './routes/Users';
import { Reciters } from './routes/Reciters';
import { Surahs } from './routes/Surahs';
import { Moods } from './routes/Moods';
import { Analytics } from './routes/Analytics';
import { Playlists } from './routes/Playlists';
import { Activity } from './routes/Activity';
import { Settings } from './routes/Settings';

const rootRoute = createRootRoute({
  component: AppLayout,
});

const indexRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/',
  component: Dashboard,
});

const usersRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/users',
  component: Users,
});

const recitersRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/reciters',
  component: Reciters,
});

const surahsRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/surahs',
  component: Surahs,
});

const moodsRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/moods',
  component: Moods,
});

const analyticsRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/analytics',
  component: Analytics,
});

const playlistsRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/playlists',
  component: Playlists,
});

const activityRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/activity',
  component: Activity,
});

const settingsRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/settings',
  component: Settings,
});

const routeTree = rootRoute.addChildren([
  indexRoute,
  usersRoute,
  recitersRoute,
  surahsRoute,
  moodsRoute,
  analyticsRoute,
  playlistsRoute,
  activityRoute,
  settingsRoute,
]);

export const router = createRouter({ routeTree, basepath: import.meta.env.BASE_URL.replace(/\/$/, '') || '/' });

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router;
  }
}
