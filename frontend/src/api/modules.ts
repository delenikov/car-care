import { http, refreshTokenStorage, tokenStorage, unwrap } from './http';
import type { Appointment, AuthResponse, Customer, DashboardSummary, DocumentRecord, LoyaltyRule, Offer, ServiceRecord, User, Vehicle } from '../types';

export interface LoginPayload {
  email: string;
  password: string;
}

export interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
}

export const authApi = {
  login: (payload: LoginPayload) => unwrap(http.post<AuthResponse>('/api/auth/login', payload)),
  refresh: () => unwrap(http.post<AuthResponse>('/api/auth/refresh', {})),
  changePassword: (payload: ChangePasswordPayload) => unwrap(http.post<void>('/api/auth/change-password', payload)),
  logout: () => {
    tokenStorage.clear();
    refreshTokenStorage.clear();
    return Promise.resolve();
  }
};

const crud = <T extends { id: string }, CreatePayload = Omit<T, 'id'>, UpdatePayload = Partial<CreatePayload>>(basePath: string) => ({
  list: () => unwrap(http.get<T[]>(basePath)),
  get: (id: string) => unwrap(http.get<T>(`${basePath}/${id}`)),
  create: (payload: CreatePayload) => unwrap(http.post<T>(basePath, payload)),
  update: (id: string, payload: UpdatePayload) => unwrap(http.put<T>(`${basePath}/${id}`, payload)),
  remove: (id: string) => unwrap(http.delete<void>(`${basePath}/${id}`))
});

export const dashboardApi = {
  summary: () => unwrap(http.get<DashboardSummary>('/api/dashboard/summary'))
};

interface BackendCustomer {
  id: string;
  fullName: string;
  email: string;
  phone?: string;
  address?: string;
}

const toCustomer = (customer: BackendCustomer): Customer => ({
  id: customer.id,
  name: customer.fullName,
  email: customer.email,
  phone: customer.phone ?? '',
  loyaltyPoints: 0,
  notes: customer.address ?? ''
});

const toCustomerRequest = (customer: Omit<Customer, 'id'>) => ({
  fullName: customer.name,
  email: customer.email,
  phone: customer.phone,
  address: customer.notes
});

export const customersApi = {
  list: () => unwrap(http.get<BackendCustomer[]>('/api/customers')).then((customers) => customers.map(toCustomer)),
  get: (id: string) => unwrap(http.get<BackendCustomer>(`/api/customers/${id}`)).then(toCustomer),
  create: (payload: Omit<Customer, 'id'>) => unwrap(http.post<BackendCustomer>('/api/customers', toCustomerRequest(payload))).then(toCustomer),
  update: (id: string, payload: Partial<Omit<Customer, 'id'>>) => unwrap(http.put<BackendCustomer>(`/api/customers/${id}`, toCustomerRequest({ name: '', phone: '', loyaltyPoints: 0, ...payload }))).then(toCustomer),
  remove: (id: string) => unwrap(http.delete<void>(`/api/customers/${id}`))
};
export const vehiclesApi = crud<Vehicle>('/api/vehicles');
export const appointmentsApi = {
  ...crud<Appointment>('/api/appointments'),
  reschedule: (id: string, startsAt: string, endsAt: string) => unwrap(http.patch<Appointment>(`/api/appointments/${id}`, { startsAt, endsAt }))
};
export const serviceRecordsApi = crud<ServiceRecord>('/api/service-records');
export const offersApi = {
  ...crud<Offer>('/api/offers'),
  send: (id: string) => unwrap(http.post<Offer>(`/api/offers/${id}/send`, {}))
};
export const documentsApi = crud<DocumentRecord>('/api/documents');
export const adminUsersApi = crud<User>('/api/admin/users');
export const loyaltyRulesApi = crud<LoyaltyRule>('/api/admin/loyalty-rules');
