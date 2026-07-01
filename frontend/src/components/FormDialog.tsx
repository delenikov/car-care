import type { FormEventHandler, ReactNode } from 'react';
import CloseRoundedIcon from '@mui/icons-material/CloseRounded';
import SaveRoundedIcon from '@mui/icons-material/SaveRounded';
import { Button, Dialog, DialogActions, DialogContent, DialogTitle, Divider, IconButton, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';

export function FormDialog({
  title,
  children,
  onClose,
  onSubmit,
  isSubmitting,
  submitLabel
}: {
  title: string;
  children: ReactNode;
  onClose: () => void;
  onSubmit: FormEventHandler<HTMLFormElement>;
  isSubmitting: boolean;
  submitLabel?: string;
}) {
  const { t } = useTranslation();

  return (
    <Dialog
      open
      fullWidth
      maxWidth="sm"
      onClose={onClose}
      PaperProps={{
        component: 'form',
        onSubmit,
        sx: {
          borderRadius: 2,
          overflow: 'hidden',
          backgroundImage: 'none'
        }
      }}
    >
      <DialogTitle component="div" sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 2, px: 3, py: 2 }}>
        <Typography variant="h5" component="h2">{title}</Typography>
        <IconButton onClick={onClose} aria-label={t('cancel')} edge="end">
          <CloseRoundedIcon />
        </IconButton>
      </DialogTitle>
      <Divider />
      <DialogContent sx={{ p: 3 }}>{children}</DialogContent>
      <Divider />
      <DialogActions sx={{ px: 3, py: 2, gap: 1 }}>
        <Button onClick={onClose} variant="outlined">
          {t('cancel')}
        </Button>
        <Button type="submit" variant="contained" startIcon={<SaveRoundedIcon />} disabled={isSubmitting}>
          {submitLabel ?? t('save')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
