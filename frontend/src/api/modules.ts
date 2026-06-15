import { http, refreshAccessToken, refreshTokenStorage, tokenStorage, unwrap } from './http';
import type { Appointment, AppointmentCancellationInfo, AppointmentSlot, AuthResponse, Customer, CustomerLoyaltyStatus, DashboardSummary, DocumentRecord, LoyaltyRule, Offer, Role, ServiceRecord, User, Vehicle } from '../types';
import { skopjeOffsetDateTime } from '../utils/dateTime';

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
  refresh: refreshAccessToken,
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
  id: string | number;
  firstName?: string;
  lastName?: string;
  fullName: string;
  email: string;
  phone?: string;
  address?: string;
}

interface BackendVehicle {
  id: string | number;
  customerId: string | number;
  customerName?: string;
  plateNumber: string;
  make: string;
  model: string;
  modelYear: number;
  vin?: string;
  fuelType?: string;
  engine?: string;
}

interface BackendServiceRecord {
  id: string;
  customerId: string;
  customerName?: string;
  vehicleId: string;
  vehiclePlate?: string;
  vehicleName?: string;
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
  id: string | number;
  customerId: string | number;
  customerName?: string;
  vehicleId: string | number;
  vehiclePlate?: string;
  vehicleName?: string;
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
  parts?: Array<{ name: string; price: number }>;
  partsCost?: number;
  laborCost?: number;
  subtotalAmount?: number;
  discountPercent?: number;
  discountAmount?: number;
  amount: number;
  status: Offer['status'];
}

interface BackendCustomerLoyaltyStatus {
  customerId: string | number;
  completedServices: number;
  requiredServices: number;
  loyal: boolean;
  discountPercent: number;
}

interface BackendDocument {
  id: string;
  customerId?: string;
  serviceRecordId?: string;
  type: string;
  fileName: string;
  contentType: string;
  storageKey: string;
  createdAt?: string;
}

const toCustomer = (customer: BackendCustomer): Customer => ({
  id: String(customer.id),
  name: customer.fullName,
  email: customer.email,
  phone: customer.phone ?? '',
  loyaltyPoints: 0,
  notes: customer.address ?? ''
});

const splitCustomerName = (name: string) => {
  const fullName = name.trim();
  const [firstName, ...rest] = fullName.split(/\s+/);
  return {
    firstName: firstName ?? fullName,
    lastName: rest.length ? rest.join(' ') : firstName ?? fullName,
    fullName
  };
};

const toCustomerRequest = (customer: Omit<Customer, 'id'>) => ({
  ...splitCustomerName(customer.name),
  email: customer.email,
  phone: customer.phone,
  address: customer.notes
});

const toVehicle = (vehicle: BackendVehicle): Vehicle => ({
  id: String(vehicle.id),
  customerId: String(vehicle.customerId),
  customerName: vehicle.customerName,
  plate: vehicle.plateNumber,
  make: vehicle.make,
  model: vehicle.model,
  year: vehicle.modelYear,
  vin: vehicle.vin,
  fuelType: vehicle.fuelType,
  engine: vehicle.engine
});

const toVehicleRequest = (vehicle: Omit<Vehicle, 'id'>) => ({
  customerId: vehicle.customerId,
  plateNumber: vehicle.plate,
  make: vehicle.make,
  model: vehicle.model,
  modelYear: vehicle.year,
  vin: vehicle.vin,
  fuelType: vehicle.fuelType,
  engine: vehicle.engine
});

const toServiceRecord = (record: BackendServiceRecord): ServiceRecord => ({
  id: record.id,
  customerId: record.customerId,
  customerName: record.customerName,
  vehicleId: record.vehicleId,
  vehiclePlate: record.vehiclePlate,
  vehicleName: record.vehicleName,
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
  id: String(appointment.id),
  customerId: String(appointment.customerId),
  customerName: appointment.customerName,
  vehicleId: String(appointment.vehicleId),
  vehiclePlate: appointment.vehiclePlate,
  vehicleName: appointment.vehicleName,
  title: appointment.serviceType,
  startsAt: appointment.scheduledAt,
  endsAt: appointment.endsAt,
  status: appointment.status,
  cancellationUrl: appointment.cancellationUrl
});

const toAppointmentRequest = (appointment: Omit<Appointment, 'id'>) => ({
  customerId: appointment.customerId,
  vehicleId: appointment.vehicleId,
  startsAt: skopjeOffsetDateTime(appointment.startsAt),
  endsAt: skopjeOffsetDateTime(appointment.endsAt),
  title: appointment.title,
  notes: undefined
});

const toOffer = (offer: BackendOffer): Offer => ({
  id: offer.id,
  customerId: offer.customerId,
  vehicleId: offer.vehicleId,
  title: offer.title,
  parts: offer.parts ?? [],
  subtotal: offer.subtotalAmount ?? ((offer.partsCost ?? 0) + (offer.laborCost ?? offer.amount)),
  discountPercent: offer.discountPercent ?? 0,
  discountAmount: offer.discountAmount ?? 0,
  total: offer.amount,
  partsCost: offer.partsCost ?? 0,
  laborCost: offer.laborCost ?? offer.amount,
  status: offer.status
});

const toOfferRequest = (offer: Omit<Offer, 'id'>) => ({
  customerId: offer.customerId,
  vehicleId: offer.vehicleId,
  title: offer.title,
  parts: offer.parts ?? [],
  partsCost: offer.partsCost,
  laborCost: offer.laborCost,
  description: undefined
});

const toCustomerLoyaltyStatus = (status: BackendCustomerLoyaltyStatus): CustomerLoyaltyStatus => ({
  customerId: String(status.customerId),
  completedServices: status.completedServices,
  requiredServices: status.requiredServices,
  loyal: status.loyal,
  discountPercent: status.discountPercent
});

const toDocument = (document: BackendDocument): DocumentRecord => ({
  id: document.id,
  customerId: document.customerId,
  serviceRecordId: document.serviceRecordId,
  title: document.fileName,
  type: document.type,
  contentType: document.contentType,
  storageKey: document.storageKey,
  uploadedAt: document.createdAt ?? new Date().toISOString()
});

export const customersApi = {
  list: (filters?: { firstName?: string; lastName?: string }) => unwrap(http.get<BackendCustomer[]>('/api/customers', { params: filters })).then((customers) => customers.map(toCustomer)),
  get: (id: string) => unwrap(http.get<BackendCustomer>(`/api/customers/${id}`)).then(toCustomer),
  create: (payload: Omit<Customer, 'id'>) => unwrap(http.post<BackendCustomer>('/api/customers', toCustomerRequest(payload))).then(toCustomer),
  update: (id: string, payload: Partial<Omit<Customer, 'id'>>) => unwrap(http.put<BackendCustomer>(`/api/customers/${id}`, toCustomerRequest({ name: '', phone: '', loyaltyPoints: 0, ...payload }))).then(toCustomer),
  remove: (id: string) => unwrap(http.delete<void>(`/api/customers/${id}`)),
  vehicles: (id: string) => unwrap(http.get<BackendVehicle[]>(`/api/customers/${id}/vehicles`)).then((vehicles) => vehicles.map(toVehicle)),
  serviceHistory: (id: string) => unwrap(http.get<BackendServiceRecord[]>(`/api/customers/${id}/service-history`)).then((records) => records.map(toServiceRecord)),
  loyaltyStatus: (id: string) => unwrap(http.get<BackendCustomerLoyaltyStatus>(`/api/customers/${id}/loyalty-status`)).then(toCustomerLoyaltyStatus)
};
export const vehiclesApi = {
  list: (filters?: { q?: string; vin?: string; plateNumber?: string; owner?: string }) => unwrap(http.get<BackendVehicle[]>('/api/vehicles', { params: filters })).then((vehicles) => vehicles.map(toVehicle)),
  get: (id: string) => unwrap(http.get<BackendVehicle>(`/api/vehicles/${id}`)).then(toVehicle),
  create: (payload: Omit<Vehicle, 'id'>) => unwrap(http.post<BackendVehicle>('/api/vehicles', toVehicleRequest(payload))).then(toVehicle),
  update: (id: string, payload: Partial<Omit<Vehicle, 'id'>>) =>
    unwrap(http.put<BackendVehicle>(`/api/vehicles/${id}`, toVehicleRequest({ customerId: '', plate: '', make: '', model: '', year: new Date().getFullYear(), fuelType: '', engine: '', ...payload }))).then(toVehicle),
  remove: (id: string) => unwrap(http.delete<void>(`/api/vehicles/${id}`)),
  serviceHistory: (id: string) => unwrap(http.get<BackendServiceRecord[]>(`/api/vehicles/${id}/service-history`)).then((records) => records.map(toServiceRecord))
};
export const appointmentsApi = {
  list: () => unwrap(http.get<BackendAppointment[]>('/api/appointments')).then((appointments) => appointments.map(toAppointment)),
  get: (id: string) => unwrap(http.get<BackendAppointment>(`/api/appointments/${id}`)).then(toAppointment),
  create: (payload: Omit<Appointment, 'id'>) => unwrap(http.post<BackendAppointment>('/api/appointments', toAppointmentRequest(payload))).then(toAppointment),
  update: (id: string, payload: Partial<Omit<Appointment, 'id'>>) =>
    unwrap(http.put<BackendAppointment>(`/api/appointments/${id}`, toAppointmentRequest({ customerId: '', vehicleId: '', title: '', startsAt: '', endsAt: '', status: 'SCHEDULED', ...payload }))).then(toAppointment),
  remove: (id: string) => unwrap(http.delete<void>(`/api/appointments/${id}`)),
  reschedule: (id: string, startsAt: string, endsAt: string) => unwrap(http.patch<BackendAppointment>(`/api/appointments/${id}`, { startsAt, endsAt })).then(toAppointment),
  available: (date: string) => unwrap(http.get<AppointmentSlot[]>('/api/appointments/available', { params: { date } })),
  sendReminders: (date: string) => unwrap(http.post<{ sent: number }>('/api/appointments/reminders', {}, { params: { date } })),
  cancel: (token: string) => unwrap(http.post<BackendAppointment>(`/api/appointments/cancel/${token}`, {})).then(toAppointment),
  publicCreate: (payload: {
    fullName: string;
    email: string;
    phone?: string;
    plateNumber: string;
    vin?: string;
    make: string;
    model: string;
    modelYear?: number;
    engine?: string;
    fuelType?: string;
    startsAt: string;
    endsAt?: string;
    serviceType: string;
    notes?: string;
  }) => unwrap(http.post<BackendAppointment>('/api/appointments/public', payload)).then(toAppointment),
  cancelInfo: (token: string) => unwrap(http.get<AppointmentCancellationInfo>(`/api/appointments/cancel-info/${token}`)),
  confirmCancel: (token: string) => unwrap(http.post<BackendAppointment>('/api/appointments/cancel', { token })).then(toAppointment)
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
    unwrap(http.put<BackendOffer>(`/api/offers/${id}`, toOfferRequest({ customerId: '', title: '', subtotal: 0, discountPercent: 0, discountAmount: 0, total: 0, parts: [], partsCost: 0, laborCost: 0, status: 'DRAFT', ...payload }))).then(toOffer),
  remove: (id: string) => unwrap(http.delete<void>(`/api/offers/${id}`)),
  send: (id: string) => unwrap(http.post<BackendOffer>(`/api/offers/${id}/send`, {})).then(toOffer),
  exportPdf: (id: string) => http.get<Blob>(`/api/offers/${id}/pdf`, { responseType: 'blob' })
};
export const documentsApi = {
  list: () => unwrap(http.get<BackendDocument[]>('/api/documents')).then((documents) => documents.map(toDocument).sort((left, right) => right.uploadedAt.localeCompare(left.uploadedAt))),
  get: (id: string) => unwrap(http.get<BackendDocument>(`/api/documents/${id}`)).then(toDocument),
  create: (payload: Omit<DocumentRecord, 'id'>) => unwrap(http.post<BackendDocument>('/api/documents', {
    customerId: payload.customerId,
    serviceRecordId: payload.serviceRecordId,
    type: payload.type,
    fileName: payload.title,
    contentType: payload.contentType ?? 'application/pdf',
    storageKey: payload.storageKey ?? `documents/${payload.title}`
  })).then(toDocument),
  update: (id: string, payload: Partial<Omit<DocumentRecord, 'id'>>) => unwrap(http.put<BackendDocument>(`/api/documents/${id}`, payload)).then(toDocument),
  remove: (id: string) => unwrap(http.delete<void>(`/api/documents/${id}`)),
  send: (id: string) => unwrap(http.post<BackendDocument>(`/api/documents/${id}/send`, {})).then(toDocument),
  exportPdf: (id: string) => http.get<Blob>(`/api/documents/${id}/pdf`, { responseType: 'blob' })
};

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
