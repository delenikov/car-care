import { useNavigate } from 'react-router-dom';
import { ChangePasswordDialog } from '../components/ChangePasswordDialog';

export function ChangePasswordPage() {
  const navigate = useNavigate();

  return <ChangePasswordDialog onClose={() => navigate('/customers', { replace: true })} />;
}
