console.error(
  `Setup unavailable: ${process.argv[2] ?? 'selected suite'} is pending its owning task.`,
);
process.exitCode = 2;
