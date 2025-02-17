<script>
  import Post from '$lib/components/post.svelte';
  import { threadStore } from '../../threadStore';
  import { onMount } from 'svelte';

  onMount(async function () {
  try {
    const response = await fetch(`https://threef.vercel.app/api/thread`);
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const data = await response.json(); 
    threadStore.set(data); 
  } catch (error) {
    console.error("Error fetching data:", error);
  }
});

</script>

{#each $threadStore.slice().sort((a, b) => b.id - a.id) as thread}
<a href={`/thread/${thread.id}`} class="w-full lg:w-[calc(33.333%-1rem)]">
  <Post
    id={thread.id}
    title={thread.title}
    description={thread.description}
    tags={thread.tags}
    imageSrc={thread.imageSrc}
    postedBy={thread.postedBy}
    postedDate={thread.postedDate}
    variant="thumb"
    resolved={thread.resolved}
  />
</a>
{/each}
