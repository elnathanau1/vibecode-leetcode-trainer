import axios from 'axios';
import type { Attempt, AttemptStatus, DailyRecommendation, HeatmapEntry, Problem, StatsProgress } from '../types';

const api = axios.create({ baseURL: '/api' });

export const problemsApi = {
  list: (params?: { difficulty?: string; pattern?: string; company?: string }) =>
    api.get<Problem[]>('/problems', { params }).then(r => r.data),
  getById: (id: number) => api.get<Problem>(`/problems/${id}`).then(r => r.data),
  create: (data: Partial<Problem>) => api.post<Problem>('/problems', data).then(r => r.data),
  getPatterns: () => api.get<string[]>('/problems/patterns').then(r => r.data),
  getCompanies: () => api.get<string[]>('/problems/companies').then(r => r.data),
};

export const attemptsApi = {
  list: () => api.get<Attempt[]>('/attempts').then(r => r.data),
  getByProblem: (problemId: number) =>
    api.get<Attempt[]>(`/attempts/problem/${problemId}`).then(r => r.data),
  create: (data: {
    problemId: number;
    status: AttemptStatus;
    timeTakenMinutes?: number;
    notes?: string;
    solvedAt?: string;
  }) => api.post<Attempt>('/attempts', data).then(r => r.data),
};

export const recommendationsApi = {
  getToday: (params?: {
    targetMinutes?: number;
    difficulty?: string;
    patterns?: string[];
    company?: string;
    categories?: string[];
  }) => api.get<DailyRecommendation>('/recommendations/today', { params }).then(r => r.data),
  generate: (data: {
    date?: string;
    targetMinutes?: number;
    forceRegenerate?: boolean;
    difficulty?: string;
    patterns?: string[];
    company?: string;
    categories?: string[];
  }) => api.post<DailyRecommendation>('/recommendations/generate', data).then(r => r.data),
};

export const spreadsheetApi = {
  importFile: (file: File) => {
    const form = new FormData()
    form.append('file', file)
    return api.post<ImportResult>('/spreadsheet/import/file', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(r => r.data)
  },
  importUrl: (url: string) =>
    api.post<ImportResult>('/spreadsheet/import/url', { url }).then(r => r.data),
  exportUrl: '/api/spreadsheet/export',
}

export interface ImportResult {
  imported: number
  skipped: number
  duplicate: number
  notFound: string[]
  errors: string[]
}

export const statsApi = {
  heatmap: (days?: number) =>
    api.get<HeatmapEntry[]>('/stats/heatmap', { params: { days } }).then(r => r.data),
  progress: () => api.get<StatsProgress>('/stats/progress').then(r => r.data),
};
