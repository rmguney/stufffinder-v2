<script>
  import { Checkbox as CheckboxPrimitive } from "bits-ui";
  import { cn } from "$lib/utils";
  let className = undefined;
  export { className as class };
  export let checked = false;
</script>

<CheckboxPrimitive.Root
  bind:checked
  class={cn(
    "peer h-4 w-4 shrink-0 rounded-sm border border-primary ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 data-[state=checked]:bg-primary data-[state=checked]:text-primary-foreground",
    className
  )}
  {...$$restProps}
>
  {#if checked}
  <CheckboxPrimitive.Indicator
    class={cn("flex items-center justify-center text-current pointer-events-none")}
  >
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="2"
      stroke-linecap="round"
      stroke-linejoin="round"
      class="h-4 w-4 pointer-events-none"
    >
      <polyline points="20 6 9 17 4 12" />
    </svg>
  </CheckboxPrimitive.Indicator>
  {/if}
</CheckboxPrimitive.Root>