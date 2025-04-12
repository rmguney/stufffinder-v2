<script>
  import "../app.css";
  import { onMount } from 'svelte';
  import PostContainer from '$lib/components/postContainer.svelte';
  import SortDropdown from '$lib/components/SortDropdown.svelte';
  import { forceRefreshThreads, initializeThreadStore, isLoading } from '../threadStore.js';

  let sortValue = 'none';

  // Initialize thread store when component mounts
  onMount(() => {
      initializeThreadStore();
  });

  function handleSortChange(value) {
    console.log('Sort changed to:', value);
    sortValue = value;
  }
</script>

<div class="flex flex-col items-center h-full min-h-screen text-text bg-change dark:bg-dark shifting p-4 lg:py-8">
  <div class="w-full lg:w-2/3">
      {#if $isLoading}
          <div class="flex justify-center items-center py-16">
              <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-rose-900"></div>
          </div>
      {:else}
          <div class="flex flex-col lg:flex-wrap lg:flex-row justify-center gap-4 lg:gap-6">
              <div class="w-full mb-4">
                <SortDropdown value={sortValue} onSortChange={handleSortChange} />
              </div>
              <PostContainer sortBy={sortValue} />
          </div>
      {/if}
  </div>
</div>