<script>
  import "../app.css";
  import { onMount } from 'svelte';
  import PostContainer from '$lib/components/postContainer.svelte';
  import { forceRefreshThreads, initializeThreadStore, isLoading } from '../threadStore.js';

  // Initialize thread store when component mounts
  onMount(() => {
      initializeThreadStore();
  });
</script>

<div class="flex flex-col items-center h-full min-h-screen text-text bg-change dark:bg-dark shifting p-4 lg:py-8">
  <div class="w-full max-w-7xl">
      {#if $isLoading}
          <div class="flex justify-center items-center py-16">
          </div>
      {:else}
          <div class="flex flex-col">
              <PostContainer />
          </div>
      {/if}
  </div>
</div>