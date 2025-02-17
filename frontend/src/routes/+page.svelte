<script>
    import "../app.css";
    import PostContainer from '$lib/components/postContainer.svelte';
</script>

  <div class="flex flex-col items-center h-full min-h-screen text-text bg-change dark:bg-dark shifting p-4 lg:py-8">
    <div class="w-full lg:w-2/3">
      <div class="flex flex-col lg:flex-wrap lg:flex-row justify-center gap-4 lg:gap-6">
        <PostContainer />
      </div>
    </div>
  </div>