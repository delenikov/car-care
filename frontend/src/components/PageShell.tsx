import { useState, type ReactNode } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  AppBar,
  Avatar,
  Box,
  Button,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Stack,
  Toolbar,
  Typography
} from '@mui/material';
import DirectionsCarRoundedIcon from '@mui/icons-material/DirectionsCarRounded';
import GroupsRoundedIcon from '@mui/icons-material/GroupsRounded';
import EventRoundedIcon from '@mui/icons-material/EventRounded';
import BuildRoundedIcon from '@mui/icons-material/BuildRounded';
import RequestQuoteRoundedIcon from '@mui/icons-material/RequestQuoteRounded';
import DescriptionRoundedIcon from '@mui/icons-material/DescriptionRounded';
import AdminPanelSettingsRoundedIcon from '@mui/icons-material/AdminPanelSettingsRounded';
import MenuRoundedIcon from '@mui/icons-material/MenuRounded';
import LockResetRoundedIcon from '@mui/icons-material/LockResetRounded';
import LogoutRoundedIcon from '@mui/icons-material/LogoutRounded';
import { useAuth } from '../auth/AuthContext';
import type { Role } from '../types';

const drawerWidth = 280;

const navItems = [
  { to: '/customers', label: 'customers', icon: <GroupsRoundedIcon /> },
  { to: '/vehicles', label: 'vehicles', icon: <DirectionsCarRoundedIcon /> },
  { to: '/appointments', label: 'appointments', icon: <EventRoundedIcon /> },
  { to: '/services', label: 'services', icon: <BuildRoundedIcon /> },
  { to: '/offers', label: 'offers', icon: <RequestQuoteRoundedIcon /> },
  { to: '/documents', label: 'documents', icon: <DescriptionRoundedIcon /> },
  { to: '/admin', label: 'admin', icon: <AdminPanelSettingsRoundedIcon />, requiredRole: 'ROLE_ADMIN' as Role }
] satisfies Array<{ to: string; label: string; icon: ReactNode; requiredRole?: Role }>;

function DrawerContent({ onNavigate }: { onNavigate?: () => void }) {
  const { t } = useTranslation();
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const displayRole = user?.roles?.[0]?.replace('ROLE_', '') ?? 'EMPLOYEE';
  const visibleNavItems = navItems.filter((item) => !item.requiredRole || user?.roles?.includes(item.requiredRole));

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  return (
    <Stack sx={{ minHeight: '100%', p: 2 }} spacing={2}>
      <Box sx={{ p: 2, borderRadius: 4, bgcolor: 'primary.dark', color: 'primary.contrastText', position: 'relative', overflow: 'hidden' }}>
        <Box sx={{ position: 'absolute', inset: 'auto -20% -40% auto', width: 140, height: 140, borderRadius: '50%', bgcolor: 'secondary.main', opacity: 0.3 }} />
        <Typography variant="h5">{t('appName')}</Typography>
        <Typography variant="body2" sx={{ opacity: 0.8 }}>
          Авто сервис центар
        </Typography>
      </Box>
      <List sx={{ flex: 1 }}>
        {visibleNavItems.map((item) => (
          <ListItemButton
            key={item.to}
            component={NavLink}
            to={item.to}
            end
            onClick={onNavigate}
            sx={{
              borderRadius: 3,
              mb: 0.5,
              '&.active': { bgcolor: 'secondary.main', color: 'secondary.contrastText' },
              '&.active .MuiListItemIcon-root': { color: 'secondary.contrastText' }
            }}
          >
            <ListItemIcon sx={{ minWidth: 42 }}>{item.icon}</ListItemIcon>
            <ListItemText primary={t(item.label)} />
          </ListItemButton>
        ))}
      </List>
      <Divider />
      <Stack direction="row" spacing={1.5} alignItems="center">
        <Avatar sx={{ bgcolor: 'primary.main' }}>{user?.fullName?.charAt(0) ?? 'C'}</Avatar>
        <Box sx={{ minWidth: 0, flex: 1 }}>
          <Typography variant="subtitle2" noWrap>
            {user?.fullName ?? 'CarCare'}
          </Typography>
          <Typography variant="caption" color="text.secondary" noWrap>
            {displayRole}
          </Typography>
        </Box>
      </Stack>
      <Button startIcon={<LockResetRoundedIcon />} component={NavLink} to="/change-password" color="primary" variant="outlined" onClick={onNavigate}>
        {t('changePassword')}
      </Button>
      <Button startIcon={<LogoutRoundedIcon />} color="primary" variant="contained" onClick={handleLogout}>
        {t('logout')}
      </Button>
    </Stack>
  );
}

export function PageShell() {
  const [open, setOpen] = useState(false);

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <AppBar position="fixed" color="transparent" elevation={0} sx={{ display: { md: 'none' }, backdropFilter: 'blur(12px)' }}>
        <Toolbar>
          <IconButton edge="start" color="primary" onClick={() => setOpen(true)}>
            <MenuRoundedIcon />
          </IconButton>
          <Typography variant="h6" sx={{ ml: 1 }}>
            CarCare
          </Typography>
        </Toolbar>
      </AppBar>
      <Drawer variant="permanent" sx={{ display: { xs: 'none', md: 'block' }, width: drawerWidth, '& .MuiDrawer-paper': { width: drawerWidth, bgcolor: 'background.paper' } }} open>
        <DrawerContent />
      </Drawer>
      <Drawer variant="temporary" open={open} onClose={() => setOpen(false)} sx={{ display: { md: 'none' }, '& .MuiDrawer-paper': { width: drawerWidth, bgcolor: 'background.paper' } }}>
        <DrawerContent onNavigate={() => setOpen(false)} />
      </Drawer>
      <Box component="main" sx={{ flex: 1, width: { md: `calc(100% - ${drawerWidth}px)` }, pt: { xs: 9, md: 0 }, px: { xs: 2, sm: 3, lg: 5 }, py: { xs: 3, md: 5 } }}>
        <Outlet />
      </Box>
    </Box>
  );
}
