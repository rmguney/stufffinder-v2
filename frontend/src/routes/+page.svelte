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

<div class="flex flex-col items-center h-full min-h-screen text-text bg-change dark:bg-dark shifting p-3 py-5">
  <div class="w-full max-w-7xl">
    <!-- Always render PostContainer, it will handle showing skeletons -->
    <div class="flex flex-col">
      <PostContainer />
    </div>
  </div>
</div>