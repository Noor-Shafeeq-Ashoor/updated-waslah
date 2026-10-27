import axios from 'axios';
// The Vite proxy forwards /api and /auth to Spring Boot in development.
export const api=axios.create({baseURL:import.meta.env.VITE_API_URL||'',timeout:15000});
api.interceptors.request.use(config=>{const token=sessionStorage.getItem('waslah_token');if(token)config.headers.Authorization=`Bearer ${token}`;return config});
export function errorText(err:unknown){if(axios.isAxiosError(err)){const d=err.response?.data;return typeof d==='string'?d:(d?.message||d?.error||err.message)}return err instanceof Error?err.message:'Something went wrong'}
export type Page<T>={content:T[];totalElements:number;totalPages:number};
export type Job={id:number;title:string;description:string;companyName:string;location:string;jobType:string;status:string;salary?:number;deadline?:string};
export type Cafe={id:number;name:string;description:string;location:string;active?:boolean};
export type Space={id:number;name:string;capacity:number;pricePerHour:number;cafeId?:number;available?:boolean};
export type Booking={id:number;studySpaceId:number;startTime:string;endTime:string;status:string;username?:string};
export type Session={id:number;title:string;description:string;capacity:number;status:string;bookingId:number;studySpaceId:number;organizerUsername:string;startTime:string;endTime:string};
export const list=<T,>(data:Page<T>|T[]):T[]=>Array.isArray(data)?data:(data?.content||[]);
