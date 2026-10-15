<script lang="ts">
  import { onDestroy, type Snippet } from 'svelte';
  let {
    open = $bindable(false),
    title,
    action,
    children,
    onconfirm,
    oncancel,
    pending = false,
  }: {
    open?: boolean;
    title: string;
    action: string;
    children: Snippet;
    onconfirm: () => void;
    oncancel?: () => void;
    pending?: boolean;
  } = $props();
  const id = $props.id();
  let dialog: HTMLDialogElement;
  let cancelButton: HTMLButtonElement;
  let opener: HTMLElement | null = null;
  function close() {
    open = false;
    oncancel?.();
  }
  $effect(() => {
    if (open && !dialog.open) {
      opener =
        document.activeElement instanceof HTMLElement
          ? document.activeElement
          : null;
      dialog.showModal();
      cancelButton.focus();
    } else if (!open && dialog.open) {
      dialog.close();
      opener?.focus();
    }
  });
  onDestroy(() => {
    if (dialog?.open) {
      dialog.close();
      opener?.focus();
    }
  });
  function keydown(event: KeyboardEvent) {
    if (event.key !== 'Tab') return;
    const controls = [
      ...dialog.querySelectorAll<HTMLElement>(
        'button:not(:disabled), a[href], input:not(:disabled), textarea:not(:disabled), select:not(:disabled), [tabindex="0"]',
      ),
    ];
    const first = controls[0];
    const last = controls.at(-1);
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last?.focus();
    }
    if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first?.focus();
    }
  }
</script>

<dialog
  bind:this={dialog}
  aria-labelledby={`${id}-title`}
  aria-describedby={`${id}-description`}
  oncancel={(event) => {
    event.preventDefault();
    close();
  }}
  onkeydown={keydown}
>
  <h2 id={`${id}-title`}>{title}</h2>
  <div id={`${id}-description`}>{@render children()}</div>
  <div class="actions">
    <button bind:this={cancelButton} class="secondary" onclick={close}
      >Cancel</button
    ><button
      class="danger"
      disabled={pending}
      aria-busy={pending}
      onclick={onconfirm}>{pending ? 'Working…' : action}</button
    >
  </div>
</dialog>
