export interface Problem {
  id: number;
  title: string;
  slug: string;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  patterns: string[];
  companies: string[];
  url: string;
  isPremium: boolean;
  estimatedMinutes: number;
  latestStatus?: 'SOLVED' | 'REVIEW' | 'FAILED';
}

export interface Attempt {
  id: number;
  problemId: number;
  problemTitle: string;
  problemSlug: string;
  problemDifficulty: string;
  solvedAt: string;
  timeTakenMinutes: number | null;
  status: 'SOLVED' | 'REVIEW' | 'FAILED';
  notes: string | null;
}

export interface RecommendedProblem {
  id: number;
  title: string;
  slug: string;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  patterns: string[];
  companies: string[];
  url: string;
  estimatedMinutes: number;
  category: 'NEW' | 'REVIEW' | 'SPACED_REPETITION' | '';
  latestStatus?: string;
}

export interface DailyRecommendation {
  id: number;
  date: string;
  targetMinutes: number;
  totalEstimatedMinutes: number;
  problems: RecommendedProblem[];
}

export interface HeatmapEntry {
  date: string;
  count: number;
}

export interface CategoryProgress {
  pattern: string;
  total: number;
  solved: number;
  percentage: number;
}

export interface StatsProgress {
  categories: CategoryProgress[];
  totalProblems: number;
  solvedProblems: number;
}

export type AttemptStatus = 'SOLVED' | 'REVIEW' | 'FAILED';
