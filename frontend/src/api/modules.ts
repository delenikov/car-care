import { http, refreshTokenStorage, tokenStorage, unwrap } from './http';
import type { Appointment, AppointmentSlot, AuthResponse, Customer, DashboardSummary, DocumentRecord, LoyaltyRule, Offer, Role, ServiceRecord, User, Vehicle } from '../types';

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
  refresh: () => {
    const refreshToken = refreshTokenStorage.get();
    return unwrap(http.post<AuthResponse>('/api/auth/refresh', { refreshToken }));
  },
  changePassword: (payload: ChangePasswordPayload) => unwrap(http.post<void>('/api/auth/change-password', payload)),
  logout: async () => {
    const refreshToken = refreshTokenStorage.get();
    if (refreshToken) {
      await unwrap(http.post<void>('/api/auth/logout', { refreshToken })).catch(() => undefined);
    }
    tokenStorage.clear();
    refreshTokenStorage.clear();
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
  firstName?: string;
  lastName?: string;
  fullName: string;
  email: string;
  phone?: string;
  address?: string;
}

interface BackendVehicle {
  id: string;
  customerId: string;
  plateNumber: string;
  make: string;
  model: string;
  modelYear: number;
  vin?: string;
}

interface BackendServiceRecord {
  id: string;
  customerId: string;
  vehicleId: string;
  serviceDate: string;
  serviceType: string;
  partsCost?: number;
  laborCost?: number;
  totalAmount: number;
  odometer: number;
  replacedParts?: string;
  notes?: string;
}

interface BackendAppointment {
  id: string;
  customerId: string;
  vehicleId: string;
  scheduledAt: string;
  endsAt: string;
  serviceType: string;
  status: Appointment['status'];
  notes?: string;
  cancellationUrl?: string;
}

interface BackendOffer {
  id: string;
  customerId: string;
  vehicleId?: string;
  title: string;
  description?: string;
  partsCost?: number;
  laborCost?: number;
  amount: number;
  status: Offer['status'];
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

const toVehicle = (vehicle: BackendVehicle): Vehicle => ({
  id: vehicle.id,
  customerId: vehicle.customerId,
  plate: vehicle.plateNumber,
  make: vehicle.make,
  model: vehicle.model,
  year: vehicle.modelYear,
  vin: vehicle.vin
});

const toVehicleRequest = (vehicle: Omit<Vehicle, 'id'>) => ({
  customerId: vehicle.customerId,
  plateNumber: vehicle.plate,
  make: vehicle.make,
  model: vehicle.model,
  modelYear: vehicle.year,
  vin: vehicle.vin
});

const toServiceRecord = (record: BackendServiceRecord): ServiceRecord => ({
  id: record.id,
  customerId: record.customerId,
  vehicleId: record.vehicleId,
  performedAt: record.serviceDate,
  mileage: record.odometer,
  summary: record.serviceType,
  cost: record.totalAmount,
  partsCost: record.partsCost ?? 0,
  laborCost: record.laborCost ?? record.totalAmount,
  replacedParts: record.replacedParts,
  notes: record.notes
});

const toServiceRecordRequest = (record: Omit<ServiceRecord, 'id'>) => ({
  customerId: record.customerId,
  vehicleId: record.vehicleId,
  serviceDate: record.performedAt,
  serviceType: record.summary,
  partsCost: record.partsCost,
  laborCost: record.laborCost,
  odometer: record.mileage,
  replacedParts: record.replacedParts,
  notes: record.notes
});

const toAppointment = (appointment: BackendAppointment): Appointment => ({
  id: appointment.id,
  customerId: appointment.customerId,
  vehicleId: appointment.vehicleId,
  title: appointment.serviceType,
  startsAt: appointment.scheduledAt,
  endsAt: appointment.endsAt,
  status: appointment.status === 'SCHEDULED' ? 'BOOKED' : appointment.status,
  cancellationUrl: appointment.cancellationUrl
});

const toAppointmentRequest = (appointment: Omit<Appointment, 'id'>) => ({
  customerId: appointment.customerId,
  vehicleId: appointment.vehicleId,
  startsAt: appointment.startsAt,
  endsAt: appointment.endsAt,
  title: appointment.title,
  notes: undefined
});

const toOffer = (offer: BackendOffer): Offer => ({
  id: offer.id,
  customerId: offer.customerId,
  vehicleId: offer.vehicleId,
  title: offer.title,
  total: offer.amount,
  partsCost: offer.partsCost ?? 0,
  laborCost: offer.laborCost ?? offer.amount,
  status: offer.status
});

const toOfferRequest = (offer: Omit<Offer, 'id'>) => ({
  customerId: offer.customerId,
  vehicleId: offer.vehicleId,
  title: offer.title,
  partsCost: offer.partsCost,
  laborCost: offer.laborCost,
  description: undefined
});

export const customersApi = {
  list: (filters?: { firstName?: string; lastName?: string }) => unwrap(http.get<BackendCustomer[]>('/api/customers', { params: filters })).then((customers) => customers.map(toCustomer)),
  get: (id: string) => unwrap(http.get<BackendCustomer>(`/api/customers/${id}`)).then(toCustomer),
  create: (payload: Omit<Customer, 'id'>) => unwrap(http.post<BackendCustomer>('/api/customers', toCustomerRequest(payload))).then(toCustomer),
  update: (id: string, payload: Partial<Omit<Customer, 'id'>>) => unwrap(http.put<BackendCustomer>(`/api/customers/${id}`, toCustomerRequest({ name: '', phone: '', loyaltyPoints: 0, ...payload }))).then(toCustomer),
  remove: (id: string) => unwrap(http.delete<void>(`/api/customers/${id}`)),
  vehicles: (id: string) => unwrap(http.get<BackendVehicle[]>(`/api/customers/${id}/vehicles`)).then((vehicles) => vehicles.map(toVehicle)),
  serviceHistory: (id: string) => unwrap(http.get<BackendServiceRecord[]>(`/api/customers/${id}/service-history`)).then((records) => records.map(toServiceRecord))
};
export const vehiclesApi = {
  list: (filters?: { vin?: string; plateNumber?: string; owner?: string }) => unwrap(http.get<BackendVehicle[]>('/api/vehicles', { params: filters })).then((vehicles) => vehicles.map(toVehicle)),
  get: (id: string) => unwrap(http.get<BackendVehicle>(`/api/vehicles/${id}`)).then(toVehicle),
  create: (payload: Omit<Vehicle, 'id'>) => unwrap(http.post<BackendVehicle>('/api/vehicles', toVehicleRequest(payload))).then(toVehicle),
  update: (id: string, payload: Partial<Omit<Vehicle, 'id'>>) =>
    unwrap(http.put<BackendVehicle>(`/api/vehicles/${id}`, toVehicleRequest({ customerId: '', plate: '', make: '', model: '', year: new Date().getFullYear(), ...payload }))).then(toVehicle),
  remove: (id: string) => unwrap(http.delete<void>(`/api/vehicles/${id}`)),
  serviceHistory: (id: string) => unwrap(http.get<BackendServiceRecord[]>(`/api/vehicles/${id}/service-history`)).then((records) => records.map(toServiceRecord))
};
export const appointmentsApi = {
  list: () => unwrap(http.get<BackendAppointment[]>('/api/appointments')).then((appointments) => appointments.map(toAppointment)),
  get: (id: string) => unwrap(http.get<BackendAppointment>(`/api/appointments/${id}`)).then(toAppointment),
  create: (payload: Omit<Appointment, 'id'>) => unwrap(http.post<BackendAppointment>('/api/appointments', toAppointmentRequest(payload))).then(toAppointment),
  update: (id: string, payload: Partial<Omit<Appointment, 'id'>>) =>
    unwrap(http.put<BackendAppointment>(`/api/appointments/${id}`, toAppointmentRequest({ customerId: '', vehicleId: '', title: '', startsAt: '', endsAt: '', status: 'BOOKED', ...payload }))).then(toAppointment),
  remove: (id: string) => unwrap(http.delete<void>(`/api/appointments/${id}`)),
  reschedule: (id: string, startsAt: string, endsAt: string) => unwrap(http.patch<BackendAppointment>(`/api/appointments/${id}`, { startsAt, endsAt })).then(toAppointment),
  available: (date: string) => unwrap(http.get<AppointmentSlot[]>('/api/appointments/available', { params: { date } })),
  sendReminders: (date: string) => unwrap(http.post<{ sent: number }>('/api/appointments/reminders', {}, { params: { date } })),
  cancel: (token: string) => unwrap(http.post<BackendAppointment>(`/api/appointments/cancel/${token}`, {})).then(toAppointment)
};
export const serviceRecordsApi = {
  list: () => unwrap(http.get<BackendServiceRecord[]>('/api/service-records')).then((records) => records.map(toServiceRecord)),
  get: (id: string) => unwrap(http.get<BackendServiceRecord>(`/api/service-records/${id}`)).then(toServiceRecord),
  create: (payload: Omit<ServiceRecord, 'id'>) => unwrap(http.post<BackendServiceRecord>('/api/service-records', toServiceRecordRequest(payload))).then(toServiceRecord),
  update: (id: string, payload: Partial<Omit<ServiceRecord, 'id'>>) =>
    unwrap(http.put<BackendServiceRecord>(`/api/service-records/${id}`, toServiceRecordRequest({ customerId: '', vehicleId: '', performedAt: '', mileage: 0, summary: '', cost: 0, partsCost: 0, laborCost: 0, ...payload }))).then(toServiceRecord),
  remove: (id: string) => unwrap(http.delete<void>(`/api/service-records/${id}`))
};
export const offersApi = {
  list: () => unwrap(http.get<BackendOffer[]>('/api/offers')).then((offers) => offers.map(toOffer)),
  get: (id: string) => unwrap(http.get<BackendOffer>(`/api/offers/${id}`)).then(toOffer),
  create: (payload: Omit<Offer, 'id'>) => unwrap(http.post<BackendOffer>('/api/offers', toOfferRequest(payload))).then(toOffer),
  update: (id: string, payload: Partial<Omit<Offer, 'id'>>) =>
    unwrap(http.put<BackendOffer>(`/api/offers/${id}`, toOfferRequest({ customerId: '', title: '', total: 0, partsCost: 0, laborCost: 0, status: 'DRAFT', ...payload }))).then(toOffer),
  remove: (id: string) => unwrap(http.delete<void>(`/api/offers/${id}`)),
  send: (id: string) => unwrap(http.post<BackendOffer>(`/api/offers/${id}/send`, {})).then(toOffer)
};
export const documentsApi = crud<DocumentRecord>('/api/documents');

export interface EmployeePayload {
  fullName: string;
  email: string;
  password?: string;
  enabled?: boolean;
  roles: Role[];
}

export const adminUsersApi = {
  list: () => unwrap(http.get<User[]>('/api/admin/users')),
  create: (payload: EmployeePayload) => unwrap(http.post<User>('/api/admin/users', payload)),
  update: (id: string, payload: EmployeePayload) => unwrap(http.put<User>(`/api/admin/users/${id}`, payload)),
  remove: (id: string) => unwrap(http.delete<void>(`/api/admin/users/${id}`))
};
export const loyaltyRulesApi = crud<LoyaltyRule>('/api/admin/loyalty-rules');
