import { state } from "../core/state.js";
import { showToast } from "../ui/toast.js";
import { openTasksModal } from "../ui/tasks-modal.js";

export function notifyTasksOnRouteIfAny(journey) {
  const tasks =
    journey && Array.isArray(journey.tasksOnRoute) ? journey.tasksOnRoute : [];
  if (!tasks.length) return;

  const journeyId = journey?.id || journey?.journeyId || null;

  if (journeyId && state.lastNotifiedJourneyId === journeyId) return;

  const sig = tasks
    .map((t) =>
      String(
        t?.taskId ||
          t?.id ||
          t?.googleTaskId ||
          t?.sourceTaskId ||
          t?.title ||
          ""
      )
    )
    .filter(Boolean)
    .sort()
    .join("|");

  if (journeyId && sig && state.lastTasksSignature === `${journeyId}:${sig}`)
    return;

  state.lastNotifiedJourneyId = journeyId;
  state.lastTasksSignature = journeyId ? `${journeyId}:${sig}` : sig;

  const count = tasks.length;
  const firstTitle = tasks[0]?.title || "a task";

  const msg =
    count === 1
      ? `Task on your route: ${firstTitle}`
      : `${count} tasks on your route`;

  showToast(msg, {
    variant: "warning",
    important: true,
    durationMs: 15000,
    actionText: "View",
    onAction: () => openTasksModal(tasks),
  });
}
