<script>
  import Post from '$lib/components/post.svelte';
  import { threadStore, forceRefreshThreads } from '../../threadStore';
  import { onMount } from 'svelte';

  onMount(async function () {
    // The initialization now happens in the parent component
    // This component just displays the current state of the store
    // console.log("PostContainer mounted, thread count:", $threadStore.length);
  });
</script>

{#each $threadStore.slice().sort((a, b) => b.id - a.id) as thread}
  <div class="w-full lg:w-[calc(33.333%-1rem)]">
    <a href={`/thread/${thread.id}`}>
      <Post
        id={thread.id}
        title={thread.title}
        description=" "
        tags={thread.tags || []}
        imageSrc={thread.mysteryObjectImage ? `data:image/png;base64,${thread.mysteryObjectImage}` : ''}
        postedBy={thread.author}
        createdAt={thread.createdAt}
        solved={thread.solved}
        mysteryObject={thread.mysteryObject || null}
        variant="thumb"
      />
    </a>
  </div>
{:else}
  <div class="w-full text-center py-8">
    <p>No posts found. Check back later!</p>
  </div>
{/each}