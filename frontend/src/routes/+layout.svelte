<script>
        import { ModeWatcher } from "mode-watcher";
        import Header from '$lib/components/header.svelte';
        import { onMount } from 'svelte';

        onMount(() => {
        if (performance.navigation.type === 1 && window.location.pathname !== '/') {
                window.location.href = '/';
        }
        });
</script>

<ModeWatcher />  
<main>
 <Header/>
 <slot/>
</main>