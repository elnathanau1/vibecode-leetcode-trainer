interface Props { difficulty: string }

export default function DifficultyBadge({ difficulty }: Props) {
  const cls = difficulty?.toLowerCase()
  return (
    <span className={`badge badge-${cls}`}>
      {difficulty}
    </span>
  )
}
