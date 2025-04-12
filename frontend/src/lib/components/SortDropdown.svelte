<script>
  import { onMount } from 'svelte';

  export let value = 'none';
  export let onSortChange = () => {};

  let isOpen = false;

  const sortOptions = [
    { value: 'none', label: 'None' },
    { value: 'recent', label: 'Recent' },
    { value: 'oldest', label: 'Oldest' },
    { value: 'trending', label: 'Most Trending' },
    { value: 'resolved', label: 'Resolved' },
    { value: 'unresolved', label: 'Unresolved' },
  ];

  function handleSortChange(newValue) {
    value = newValue;
    onSortChange(newValue);
    isOpen = false;
  }

  $: currentLabel = sortOptions.find(option => option.value === value)?.label || 'None';

  // Close dropdown when clicking outside
  let dropdownRef;
  function handleClickOutside(event) {
    if (dropdownRef && !dropdownRef.contains(event.target)) {
      isOpen = false;
    }
  }

  onMount(() => {
    document.addEventListener('click', handleClickOutside);
    return () => {
      document.removeEventListener('click', handleClickOutside);
    };
  });
</script>

<div class="relative inline-block" bind:this={dropdownRef}>
  <button
    type="button"
    on:click|stopPropagation={() => isOpen = !isOpen}
    class="inline-flex items-center justify-between w-32 px-3 py-1.5 text-xs font-medium bg-white dark:bg-neutral-900 text-gray-900 dark:text-gray-100 border border-neutral-200 dark:border-neutral-700 rounded-md shadow-sm hover:bg-neutral-50 dark:hover:bg-neutral-800 focus:outline-none"
  >
    <span class="truncate">{currentLabel}</span>
    <svg
      class="w-4 h-4 ml-1 text-gray-500 dark:text-gray-400"
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 20 20"
      fill="currentColor"
    >
      <path
        fill-rule="evenodd"
        d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z"
        clip-rule="evenodd"
      />
    </svg>
  </button>

  {#if isOpen}
    <div 
      class="absolute right-0 z-50 w-32 mt-1 bg-white dark:bg-neutral-900 border border-neutral-200 dark:border-neutral-700 rounded-md shadow-lg"
    >
      {#each sortOptions as option}
        <button
          type="button"
          on:click={() => handleSortChange(option.value)}
          class="w-full text-left px-3 py-1.5 text-xs text-gray-900 dark:text-gray-100 hover:bg-neutral-50 dark:hover:bg-neutral-800 {option.value === value ? 'bg-neutral-50 dark:bg-neutral-800 font-medium' : ''}"
        >
          {option.label}
        </button>
      {/each}
    </div>
  {/if}
</div> 